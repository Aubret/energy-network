package modele;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="prosumer")
public class prosumer {
    @XmlAttribute(name="id")
    private int id ;

    public prosumer(){}
}
