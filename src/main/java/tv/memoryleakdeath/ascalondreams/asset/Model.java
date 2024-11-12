package tv.memoryleakdeath.ascalondreams.asset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Model implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private List<Material> materialList;
    private List<Entity> entities = new ArrayList<>();

    public Model(String id, List<Material> materialList) {
        this.id = id;
        this.materialList = materialList;
    }

    public String getId() {
        return id;
    }

    public List<Material> getMaterialList() {
        return materialList;
    }

    public List<Entity> getEntities() {
        return entities;
    }

}
