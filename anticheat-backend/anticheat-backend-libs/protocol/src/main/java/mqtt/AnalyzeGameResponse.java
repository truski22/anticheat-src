package mqtt;

import java.util.List;

public class AnalyzeGameResponse extends Common {
    private boolean legal;
    private List<Integer> white;
    private List<Integer> black;

    public boolean isLegal() {
        return legal;
    }

    public void setLegal(boolean legal) {
        this.legal = legal;
    }

    public List<Integer> getWhite() {
        return white;
    }

    public void setWhite(List<Integer> white) {
        this.white = white;
    }

    public List<Integer> getBlack() {
        return black;
    }

    public void setBlack(List<Integer> black) {
        this.black = black;
    }
}
