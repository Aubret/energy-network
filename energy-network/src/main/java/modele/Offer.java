package modele;

public class Offer {
    private static int compteur=0 ;
    private int id ;
    private int amount ;
    private double quantity ;
    private double losses ;
    private double congestion_cost ;
    private double totalQuantity ;
    private boolean evaluate ; // Si l'offre a été étudiée

    public Offer(int amount, double quantity){
        this.setAmount(amount);
        this.setQuantity(quantity);
        this.evaluate = false ;
        this.compteur++ ;
        this.id = this.compteur ;

    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getLosses() {
        return losses;
    }

    public void setLosses(double losses) {
        this.losses = losses;
    }

    public double getCongestion_cost() {
        return congestion_cost;
    }

    public void setCongestion_cost(double congestion_cost) {
        this.congestion_cost = congestion_cost;
    }

    public double getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(double totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public boolean isEvaluate() {
        return evaluate;
    }

    public void setEvaluate(boolean evaluate) {
        this.evaluate = evaluate;
    }

    public int getId(){
        return this.id ;
    }

    public boolean equals(Offer offer){
        return this.getId() == offer.getId() ;
    }
}
