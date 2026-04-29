package service;

import java.util.List;
import model.PlayerPerformance;

// Design Pattern: Strategy (OCP) for role-wise consistency formulas.
public interface ConsistencyStrategy {
    double compute(List<PlayerPerformance> performances);
}
