public class TipCalculator {
    private final double totalBill;
    private final double tipPercent;
    private final int people;

    public TipCalculator(double totalBill, double tipPercent, int people) {
        this.totalBill = totalBill;
        this.tipPercent = tipPercent;
        this.people = people;
    }

    public double calculateTipAmount() {
        return totalBill * tipPercent / 100.0;
    }

    public double calculateTotalWithTip() {
        return totalBill + calculateTipAmount();
    }

    public double calculateEachPays() {
        return calculateTotalWithTip() / people;
    }
}
