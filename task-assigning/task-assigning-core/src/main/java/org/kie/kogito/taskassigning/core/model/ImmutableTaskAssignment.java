package org.kie.kogito.taskassigning.core.model;

public class ImmutableTaskAssignment extends TaskAssignment {

    public ImmutableTaskAssignment() {
        // required for marshaling and FieldAccessingSolutionCloner purposes.
    }

    public ImmutableTaskAssignment(ImmutableTask task, boolean pinned) {
        super(task);
        super.setPinned(pinned);
    }

    @Override
    public void setPinned(boolean pinned) {
        // can never be changed.
    }
}
