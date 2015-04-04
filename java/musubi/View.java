package musubi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A view of a substitution. This may or may not be the same as a raw substitution. A raw
 * substitution often has unused variables, and/or variables that only appear once and ought to be
 * expanded.
 */
public final class View {
  /**
   * Builder of {@code View} instances.
   */
  public static final class Builder {
    private Set<Var> requestedVars = new HashSet<>();
    private Subst subst;

    public Builder addRequestedVar(Var... requestedVars) {
      return addAllRequestedVars(Arrays.asList(requestedVars));
    }

    public Builder addAllRequestedVars(Iterable<? extends Var> requestedVars) {
      for (Var var : requestedVars) {
        this.requestedVars.add(var);
      }
      return this;
    }

    public Builder setSubst(Subst subst) {
      if (this.subst != null) {
        throw new IllegalStateException();
      }
      this.subst = subst;
      return this;
    }

    /**
     * Builds a view with variables that only appear once out of all
     * substitutions replaced with their mapping, and their mapping removed. Also, removes variables
     * from the substitution mapping that are not used.
     */
    public View build() {
      View view = new View(subst.map());

      while (true) {
        View simpler = view.simplify(requestedVars);

        if (simpler.map.size() == view.map.size()) {
          return simpler;
        }

        view = simpler;
      }
    }
  }

  private final Map<Var, Object> map;

  private View(Map<Var, Object> map) {
    this.map = map;
  }

  private static void countOccurrences(Map<Var, int[]> occurrences, Object value) {
    if (value instanceof Var) {
      if (!occurrences.containsKey(value)) {
        occurrences.put((Var) value, new int[] {1});
      } else {
        occurrences.get(value)[0]++;
      }
    } else if (value instanceof Cons) {
      Cons cons = (Cons) value;
      countOccurrences(occurrences, cons.car());
      countOccurrences(occurrences, cons.cdr());
    }
  }

  private Object replace(Map<Var, int[]> occurrences, Object value) {
    if (value instanceof Var) {
      int[] thisCount = occurrences.get(value);
      if ((thisCount != null) && (thisCount[0] == 1) && map.containsKey(value)) {
        return replace(occurrences, map.get(value));
      }
    } else if (value instanceof Cons) {
      Cons cons = (Cons) value;
      return new Cons(
          replace(occurrences, cons.car()),
          replace(occurrences, cons.cdr()));
    }

    return value;
  }

  private View simplify(Set<Var> requestedVars) {
    Map<Var, int[]> occurrences = new HashMap<>();

    for (Var requestedVar : requestedVars) {
      occurrences.put(requestedVar, new int[] {1});
    }

    for (Object value : map.values()) {
      countOccurrences(occurrences, value);
    }

    Map<Var, Object> newMap = new HashMap<>();
    for (Var var : map.keySet()) {
      if (!occurrences.containsKey(var)
          || ((occurrences.get(var)[0] == 1) && !requestedVars.contains(var))) {
        continue;
      }

      newMap.put(var, replace(occurrences, map.get(var)));
    }

    return new View(newMap);
  }

  @Override
  public String toString() {
    return map.toString();
  }
}