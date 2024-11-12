package tv.memoryleakdeath.ascalondreams.asset;

import java.io.Serializable;

public class Texture implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String path;

    public Texture(int id, String path) {
        super();
        this.id = id;
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

}
