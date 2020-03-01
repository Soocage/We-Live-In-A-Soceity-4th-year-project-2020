package math;

import java.util.Objects;

public class Vector4f extends Vector3f {
  private float vectorW;

  public Vector4f(float x, float y, float z, float w) {
    super(x, y, z);
    this.vectorW = w;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    Vector4f vector4f = (Vector4f) o;
    return Float.compare(vector4f.vectorW, vectorW) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), vectorW);
  }

  public float getW() {
    return vectorW;
  }
}