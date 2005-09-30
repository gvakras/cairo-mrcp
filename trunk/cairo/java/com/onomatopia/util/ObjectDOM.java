package com.onomatopia.util;

import java.lang.reflect.Method;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

public class ObjectDOM {

    private static final short MAX_DEPTH = 50;

    public static Element toXML(String name, Object object, int depth) {
        Element root = new Element(name);
        if (depth++ < MAX_DEPTH) {
            String classname = (object == null) ? "null" : object.getClass().getName();
            root.setAttribute("class", classname);
            if (object == null) {
                 root.setText("null");
            } else if (stringable(object)) {
                root.setText(object.toString());
            } else if (object instanceof Collection) {
                int i = 0;
                for (Iterator it = ((Collection) object).iterator(); it.hasNext();) {
                    Element element = toXML("element", it.next(), depth);
                    element.setAttribute("index", new Integer(i++).toString());
                    root.addContent(element);
                }
            } else if (object instanceof Map) {
                Map map = (Map) object;
                for (Iterator it = map.keySet().iterator(); it.hasNext();) {
                    Object key = it.next();
                    Element element = toXML("key", key, depth);
                    root.addContent(element);
                    element = toXML("value", map.get(key), depth);
                    root.addContent(element);
                }
            } else {
                Method[] methods = object.getClass().getDeclaredMethods();
                for (int i = 0; i < methods.length; i++) {
                    String methodName = methods[i].getName();
                    if (methodName.startsWith("get") && methods[i].getParameterTypes().length == 0
                                                     && !methods[i].getReturnType().getName().equals("void")) {
                        try {
                            Object result = methods[i].invoke(object, (Object[]) null);
                            Element element = toXML(methodName, result, depth);
                            root.addContent(element);
                        } catch (Exception e) {
    //                        Element element = new Element(methodName);
    //                        element.setText("Exception encountered: " + e.getMessage());
    //                        root.addContent(element);
                        }
                    } else {
    //                    Element element = new Element(methodName);
    //                    element.setText("Method not invoked!");
    //                    root.addContent(element);
                    }
                }
            }
        }
        return root;
    }

    private static boolean stringable(Object object) {
        return (object instanceof String) || (object instanceof StringBuffer) || (object instanceof Number);
    }

    public static void outputToXML(String name, Object object) {
        outputToXML(name, object, "object_contents.xml");
    }

    public static void outputToXML(String name, Object object, String filename) {
        try {
            Element element = toXML(name, object, 0);
            Document doc = new Document(element);
            // Output the document, use standard formatter
            XMLOutputter out = new XMLOutputter();
            out.output(doc, new FileOutputStream(filename));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}