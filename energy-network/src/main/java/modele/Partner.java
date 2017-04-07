package modele;

public class Partner {
    private Prosumer buyer ;
    private Prosumer seller ;
    private Offer buyerProposition ;
    private Offer sellerProposition ;
    private Offer lastProposition ;

    public Partner(Prosumer buyer, Prosumer seller){
        this.setBuyer(buyer);
        this.setSeller(seller);
    }

    public Offer getBuyerProposition() {
        return buyerProposition;
    }

    public void setBuyerProposition(Offer buyerProposition) {
        this.buyerProposition = buyerProposition;
        this.lastProposition = buyerProposition;
    }

    public Offer getSellerProposition() {
        return sellerProposition;
    }

    public void setSellerProposition(Offer sellerProposition) {
        this.sellerProposition = sellerProposition;
        this.lastProposition = sellerProposition ;
    }

    public Prosumer hasToOffer(){
        if( this.lastProposition.equals(sellerProposition) )
            return this.getSeller();
        if(this.lastProposition.equals(buyerProposition))
            return this.getBuyer();
        return null ;
    }

    public Prosumer getBuyer() {
        return buyer;
    }

    public void setBuyer(Prosumer buyer) {
        this.buyer = buyer;
    }

    public Prosumer getSeller() {
        return seller;
    }

    public void setSeller(Prosumer seller) {
        this.seller = seller;
    }
}
