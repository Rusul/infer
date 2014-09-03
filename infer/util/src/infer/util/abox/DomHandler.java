package infer.util.abox;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Handler for Dom xml messages
 *
 * @author Brian Freyling
 * @version 1.0
 */
public class DomHandler {
	/** logger */
	private static Log logger = LogFactory.getLog(DomHandler.class);

	/**
	 * Document for parsing
	 */
	private Document dom;
    public Document getDom(){ return dom; }
    private void setDom(Document dom){ this.dom = dom; }

	/**
	 * The File that contains the message for import
	 */
    private File file;
    protected File getFile(){ return file; }
    private void setFile(File f) { this.file = f; }


    /**
     * MessageImporterImpl constructor that sets the file for importing the message.
     * @param file - the file containing the xml message to be imported.
     */
    public DomHandler(File file){
        setFile(file);
        setupDOM();
    }

    /**
     * Get one node by name.
     * If there are multiple nodes by the provided name it is logged to the jboss log,
     * and a random node is selected.
     * @param nodeName
     * @return A node by the provided name. Or null if the node does not exist.
     */
    public Node getSingleNodeByNodeName(String nodeName) {
    	NodeList elements = dom.getElementsByTagName(nodeName);
    	if (elements.getLength() > 1)
        	logger.error("There is more then one " + nodeName
        			+ " element, this method will only return the value "
        			+ "of the first one.");
        
        Node temp = elements.item(0);
        
        if (logger.isDebugEnabled()) logger.debug("ingested " + nodeName);
        return temp;
    }
    
	/**
	 * Takes a current node and finds all child nodes with the node name.
	 * @param currentNode - the node to be searched for children with the name.
	 * @param nodeName - the text name of the node
	 * @return a list of nodes or empty list if no nodes are found
     */
    public List<Node> getChildNodesByNodeName(Node currentNode, String nodeName) {    	
        List<Node> nodeList = new LinkedList<Node>();
        if(currentNode == null) {return nodeList;}
        
        Node node = currentNode.getFirstChild();
        if (node != null) {
            do {
        	     if (nodeName.equals(node.getNodeName())){
                     nodeList.add(node);
        	     }
        	     node = node.getNextSibling();
            } while (node != null);
        }
        return nodeList;
    }
    
    /**
     * Get the value of the first child node
     * @param node to find child from
     * @return String, child Node Value if present "" otherwise;
     */
    public String getFirstChildNodeValue(Node node) {
    	if(node == null) {return "";}
    	String result = "";
    	Node child = node.getFirstChild();
    	if (child != null) {
    		result = child.getNodeValue();
    	}
    	return result;
    }
    
    /**
     * Get Attributes of the Node
     * @param node to get attributes from
     * @return NamedNodeMap of attributes, null if the node is null
     */
    public NamedNodeMap getAttributes(Node node) {
    	if (node == null) return null;
    	return node.getAttributes();
    }
    
    /**
     * Get the value of Named Attribute
     * @param map of attributes
     * @param name of the desired attribute
     * @return value of Named Attribute, "" if map is null or attribute is not found
     */
    public String getNamedAttributeValue(NamedNodeMap map, String name) {
    	if(map == null) {return "";}
    	String result = "";
    	Node named = map.getNamedItem(name);
    	if(named != null) {
    		result = named.getNodeValue();
    	}
    	return result;
    }

    /**
     * Get the value of the first child of the node by the provided Name.
     * If there is more then one Node by the provided name a random one is selected.
     * @param nodeName Name of the node
     * @return Value of the First Child of the named node. Returns "" if not found.
     */
    public String getSingleElementsByTagName(String nodeName) {
	    Node node = getSingleNodeByNodeName(nodeName);
	    String temp = getFirstChildNodeValue(node);
        return temp;
    }
    
    public NodeList getElementsByTagName(String tagName) {
    	NodeList list = dom.getElementsByTagName(tagName);
    	return list;
    }
    
    /**
     * Finds the first child node of the name provided.
     *
     * @param currentNode the current node to start the search from
     * @param nodeName - the name of the node type to find
     * @return the first instance of a child with the name provided or null
     */
    public Node findNamedChildNode(Node currentNode, String nodeName){
    	if(currentNode == null) {return null;}
    	NodeList viChildren = currentNode.getChildNodes();
    	if(viChildren == null) {return null;}
    	for (int i = 0; i < viChildren.getLength(); i++){
            Node viNode = viChildren.item(i);
            if (nodeName.equals(viNode.getNodeName())){
                return viNode;
            }
    	}
    	return null;
    }

    /**
     * Removes the list of nodes from the current dom.
     *
     * @param elementName - name of element nodes to remove
     */
    private void removeNodes(String elementName){
    	NodeList nl = dom.getElementsByTagName(elementName);
        if (nl != null) {
		    for (int i = 0; i < nl.getLength(); i++){
			    Node node = nl.item(i);
			
			    Node parent = node.getParentNode();
			
			    parent.removeChild(node);
		    }
		}
    }

    /**
     * Sets up the DOM Parser
     */
    private void setupDOM(){
		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			//Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
			if (logger.isDebugEnabled()){
			    logger.debug("in impl: the file is " + this.file);
                logger.debug("in impl: the doc builder is " + db);
    			logger.debug("in impl: " + dom);
			}
			//parse using builder to get DOM representation of the XML file
			setDom(db.parse(this.file));
		}catch(ParserConfigurationException pce) {
			logger.error(pce);
		}catch(SAXException se) {
			logger.error(se);
		}catch(IOException ioe) {
			logger.error(ioe);
		}
    }  
}