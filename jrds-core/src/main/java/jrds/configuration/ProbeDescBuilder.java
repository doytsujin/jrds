package jrds.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.GenericBean;
import jrds.GraphDesc;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

public class ProbeDescBuilder extends ConfigObjectBuilder<ProbeDesc<? extends Object>> {
    static final private Logger logger = LoggerFactory.getLogger(ProbeDescBuilder.class);

    private ClassLoader classLoader = ProbeDescBuilder.class.getClassLoader();
    private Map<String, GraphDesc> graphDescMap = Collections.emptyMap();

    public ProbeDescBuilder() {
        super(ConfigType.PROBEDESC);
    }

    @Override
    ProbeDesc<? extends Object> build(JrdsDocument n) throws InvocationTargetException {
        try {
            return makeProbeDesc(n);
        } catch (SecurityException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | 
                ClassNotFoundException | NoClassDefFoundError | InstantiationException e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public <KeyType> ProbeDesc<KeyType> makeProbeDesc(JrdsDocument n) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException {
        ProbeDesc<KeyType> pd = new ProbeDesc<KeyType>();

        JrdsElement root = n.getRootElement();
        setMethod(root.getElementbyName("probeName"), pd, "setProbeName");
        setMethod(root.getElementbyName("name"), pd, "setName");
        setMethod(root.getElementbyName("index"), pd, "setIndex");

        logger.trace("Creating probe description {}", pd.getName());

        JrdsElement classElem = root.getElementbyName("probeClass");
        if(classElem == null) {
            throw new RuntimeException("Probe " + pd.getProbeName() + "defined without class");
        }
        String className = classElem.getTextContent().trim();
        Class<? extends Probe<?, ?>> c = (Class<? extends Probe<?, ?>>) classLoader.loadClass(className);
        pd.setProbeClass((Class<? extends Probe<KeyType, ?>>) c);

        setMethod(root.getElementbyName("uptimefactor"), pd, "setUptimefactor", Float.TYPE);

        boolean withgraphs = false;
        JrdsElement graphsElement = root.getElementbyName("graphs");
        if(graphsElement != null) {
            for(JrdsElement e: graphsElement.getChildElementsByName("name")) {
                String graphName = e.getTextContent();
                graphName = graphName != null ? graphName.trim() : "";
                if(graphDescMap.containsKey(graphName)) {
                    pd.addGraph(graphName);
                    withgraphs = true;
                    logger.trace("Adding graph: {}", graphName);
                } else {
                    logger.info("Missing graph {} for probe {}", graphName, pd.getName());
                }
            }
        }
        if(!withgraphs) {
            logger.debug("No graph defined for probe {}", pd.getName());
        }

        for(JrdsElement specificNode: root.getChildElementsByName("specific")) {
            Map<String, String> m = specificNode.attrMap();
            if(m != null) {
                String name = m.get("name");
                String value = specificNode.getTextContent().trim();
                pd.addSpecific(name, value);
                logger.trace("Specific added: {}='{}'", name, value);
            }
        }

        JrdsElement requesterElement = root.getElementbyName("snmpRequester");
        if(requesterElement != null) {
            String snmpRequester = requesterElement.getTextContent();
            if(snmpRequester != null) {
                snmpRequester = snmpRequester.trim();
                if(!snmpRequester.isEmpty()) {
                    pd.addSpecific("requester", snmpRequester);
                    logger.trace("Specific added: requester='{}'", snmpRequester);
                }
            }
        }

        // Populating the custom beans map
        for(JrdsElement attr: root.getChildElementsByName("customattr")) {
            String beanName = attr.getAttribute("name");
            pd.addBean(new GenericBean.CustomBean(beanName));
        }

        // Populating the default arguments map
        JrdsElement argsNode = root.getElementbyName("defaultargs");
        if(argsNode != null) {
            for(JrdsElement attr: argsNode.getChildElementsByName("attr")) {
                String beanName = attr.getAttribute("name");
                String finalBeanString = attr.getAttribute("delayed");
                boolean finalBean = "true".equalsIgnoreCase(finalBeanString);
                String beanValue = attr.getTextContent();
                pd.addDefaultBean(beanName, beanValue, finalBean);
            }
        }
        doDsList(pd.getName(), root).forEach(pd::add);

        return pd;
    }

    /**
     * @param classLoader the classLoader to set
     */
    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setGraphDescMap(Map<String, GraphDesc> graphDescMap) {
        this.graphDescMap = graphDescMap;
    }

}
