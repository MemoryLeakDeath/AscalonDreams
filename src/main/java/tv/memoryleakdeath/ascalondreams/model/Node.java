package tv.memoryleakdeath.ascalondreams.model;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class Node {
   private final List<Node> children = new ArrayList<>();
   private final String name;
   private final Matrix4f nodeTransform;
   private final Node parent;

   public Node(String name, Node parent, Matrix4f nodeTransform) {
      this.name = name;
      this.parent = parent;
      this.nodeTransform = nodeTransform;
   }

   public void addChild(Node node) {
      this.children.add(node);
   }

   public List<Node> getChildren() {
      return children;
   }

   public String getName() {
      return name;
   }

   public Matrix4f getNodeTransform() {
      return nodeTransform;
   }

   public Node getParent() {
      return parent;
   }
}
