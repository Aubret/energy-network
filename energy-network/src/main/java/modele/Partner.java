package modele;

public class Partner {
    private Prosumer buyer ;
    private Prosumer seller ;
    private Offer buyerProposition ;
    private Offer sellerProposition ;
    private Offer lastProposition ;
    private Object buyerData ; // To store zeuthen indexes
    private Object sellerData ;
    private boolean toConsider ; // Empêche de faire des concessions à un même prosumer juste après avoir accepté un deal de lui

    public Offer getLastProposition() {
        return lastProposition;
    }

    public Partner(Prosumer buyer, Prosumer seller){
        this.setBuyer(buyer);
        this.setSeller(seller);
        this.setToConsider(true);
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
            return this.getBuyer();
        if(this.lastProposition.equals(buyerProposition))
            return this.getSeller();
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

    public Object getBuyerData() {
        return buyerData;
    }

    public void setBuyerData(Object buyerData) {
        this.buyerData = buyerData;
    }

    public Object getSellerData() {
        return sellerData;
    }

    public void setSellerData(Object sellerData) {
        this.sellerData = sellerData;
    }

    public boolean isToConsider() {
        return toConsider;
    }

    public void setToConsider(boolean toConsider) {
        this.toConsider = toConsider;
    }
}
