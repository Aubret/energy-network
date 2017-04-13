package modele;

public class Accord {

    private Offer offer ;
    private Partner partner ;

    public Accord(Offer offer, Partner partner){
        this.setOffer(offer);
        this.setPartner(partner);
    }

    public Offer getOffer() {
        return offer;
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
    }

    public Partner getPartner() {
        return partner;
    }

    public void setPartner(Partner partner) {
        this.partner = partner;
    }
}
