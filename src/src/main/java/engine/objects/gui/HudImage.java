package engine.objects.gui;

import engine.Window;
import engine.graphics.Vertex3D;
import engine.graphics.mesh.dimension.two.RectangleMesh;
import engine.graphics.renderer.GuiRenderer;
import engine.tools.MousePicker;
import math.Vector2f;
import math.Vector3f;

public class HudImage {
  private Vector2f position = new Vector2f(0, 0);
  private Vector2f scale = new Vector2f(1, 1);
  private Vector3f rotation = new Vector3f(0, 0, 0);
  private RectangleMesh mesh;
  private float edgeX;
  private float offsetX;
  private float edgeY;
  private float offsetY;

  /**
   * Instantiates a new Hud object.
   *
   * @param mesh the mesh.
   */
  public HudImage(RectangleMesh mesh,
                  float edgeX,
                  float offsetX,
                  float edgeY,
                  float offsetY) {
    this.mesh = mesh;
    this.edgeX = edgeX;
    this.offsetX = offsetX;
    this.edgeY = edgeY;
    this.offsetY = offsetY;
    // reposition to accommodate for window span
    reposition();
  }

  public Vector3f getRotation() {
    return rotation;
  }

  public void setRotation(Vector3f rotation) {
    this.rotation = rotation;
  }

  /**
   * Get normalised vertex positions vector 2 f [ ].
   *
   * @return the vector 2 f [ ]
   */
  public Vector2f[] getNormalisedVertexPositions() {
    Vertex3D[] vertices = mesh.getModel().getVertices();
    // Get Array of X and Y offsets for all vertices
    Vector2f[] vertexPositions = new Vector2f[vertices.length];
    for (int i = 0; i < vertices.length; i++) {
      Vector3f vertexPosition = vertices[i].getPosition();
      vertexPositions[i] = new Vector2f(vertexPosition.getX(), vertexPosition.getY());
    }

    // Add vertex positions to position in order to get their OpenGl coordinates
    for (int i = 0; i < vertexPositions.length; i++) {
      vertexPositions[i] = Vector2f.add(position, vertexPositions[i]);
      vertexPositions[i] = Vector2f.divide(
          vertexPositions[i],
          new Vector2f(Window.getSpanX(), Window.getSpanY()));
    }

    return vertexPositions;
  }

  /**
   * Is mouse over boolean.
   *
   * @param window the window
   * @return the boolean
   */
  public Boolean isMouseOver(Window window) {
    // Get normalised Mouse Positions
    Vector2f normalisedMouse = MousePicker.getNormalisedDeviceCoordinates(window);
    // Get normalised Vertex Positions
    Vector2f[] guiVertexPositions = getNormalisedVertexPositions();

    // Check if There are a valid amount of vertices
    if (guiVertexPositions.length >= 3) {
      float normalX = normalisedMouse.getX();
      float normalY = normalisedMouse.getY();
      // Set initial values to be that of the first Vertex
      float minX = guiVertexPositions[0].getX();
      float maxX = guiVertexPositions[0].getX();
      float minY = guiVertexPositions[0].getY();
      float maxY = guiVertexPositions[0].getY();

      // Calculate min and max values
      for (int i = 1; i < guiVertexPositions.length; i++) {
        if (guiVertexPositions[i].getX() < minX) {
          minX = guiVertexPositions[i].getX();
        }
        if (guiVertexPositions[i].getX() > maxX) {
          maxX = guiVertexPositions[i].getX();
        }
        if (guiVertexPositions[i].getY() < minY) {
          minY = guiVertexPositions[i].getY();
        }
        if (guiVertexPositions[i].getY() > maxY) {
          maxY = guiVertexPositions[i].getY();
        }
      }

      Boolean withinX = ((normalX >= minX) && (normalX <= maxX));
      Boolean withinY = ((normalY >= minY) && (normalY <= maxY));
      // Check if Mouse is within boundaries
      return (withinX && withinY);
    } else {
      return false;
    }
  }

  public float getEdgeX() {
    return edgeX;
  }

  public void setEdgeX(float edgeX) {
    this.edgeX = edgeX;
  }

  public float getOffsetX() {
    return offsetX;
  }

  public void setOffsetX(float offsetX) {
    this.offsetX = offsetX;
  }

  public float getEdgeY() {
    return edgeY;
  }

  public void setEdgeY(float edgeY) {
    this.edgeY = edgeY;
  }

  public float getOffsetY() {
    return offsetY;
  }

  public void setOffsetY(float offsetY) {
    this.offsetY = offsetY;
  }

  public Vector2f getPosition() {
    return position;
  }

  public void setPosition(Vector2f position) {
    this.position = position;
  }

  public Vector2f getScale() {
    return scale;
  }

  public void setScale(Vector2f scale) {
    this.scale = scale;
  }

  public RectangleMesh getMesh() {
    return mesh;
  }

  public void create() {
    mesh.create();
  }

  public void destroy() {
    mesh.destroy();
  }

  public void reposition() {
    position.setX(edgeX * Window.getSpanX() + offsetX);
    position.setY(edgeY * Window.getSpanY() + offsetY);
  }

  public void render(GuiRenderer renderer) {
    renderer.renderObject(this);
  }
}
