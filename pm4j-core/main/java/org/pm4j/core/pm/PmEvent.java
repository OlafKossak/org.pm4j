package org.pm4j.core.pm;

import java.util.EventObject;


/**
 * Informs about presentation model events.
 *
 * @author olaf boede
 */
public class PmEvent extends EventObject {

  private static final long serialVersionUID = 4673929102728746424L;

  public static final int VALUE_CHANGE = 1;
  public static final int TITLE_CHANGE = 1 << 1;
  public static final int TOOLTIP_CHANGE = 1 << 2;
  public static final int VISIBILITY_CHANGE = 1 << 3;
  public static final int ENABLEMENT_CHANGE = 1 << 4;
  public static final int OPTIONSET_CHANGE = 1 << 5;
  /**
   * Will be fired whenever an attribute value changes to from the initial value
   * or back again to the initial value.
   */
  public static final int VALUE_CHANGED_STATE_CHANGE = 1 << 6;
  public static final int VALIDATION_STATE_CHANGE = 1 << 7;
  public static final int STYLECLASS_CHANGE = 1 << 8;
  public static final int EXEC_COMMAND = 1 << 9;

  public static final int ALL_CHANGE_EVENTS =
    VALUE_CHANGE | TITLE_CHANGE | TOOLTIP_CHANGE |
    VISIBILITY_CHANGE | ENABLEMENT_CHANGE |
    OPTIONSET_CHANGE | VALUE_CHANGED_STATE_CHANGE | VALIDATION_STATE_CHANGE |
    STYLECLASS_CHANGE ;

  public static final int ALL = ALL_CHANGE_EVENTS | EXEC_COMMAND;

  /**
   * A bit mask for the change kind.
   */
  public final int changeKind;

  /**
   * The {@link PmObject} that is related to the event.
   * May be <code>null</code> if the event is not related to a particular PM.
   */
  public final PmObject pm;

  /**
   * @param eventSource
   *          The control or command that triggered the change.<br>
   *          Should not be <code>null</code>.
   * @param changeKind
   *          A bit mask for the change kind.
   */
  public PmEvent(Object eventSource, PmObject pm, int changeKind) {
    super(eventSource);

    this.changeKind = changeKind;
    this.pm = pm;
  }

  /**
   * @return The command that triggered the change.
   */
  public PmCommand getChangingCommand() {
    Object src = super.getSource();
    return src instanceof PmCommand
              ? (PmCommand)src
              : null;
  }

  /**
   * @return A bit mask for the change kind.
   */
  public final int getChangeKind() {
    return changeKind;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[source=" + source + ", changeKind=" + changeKind + "]";
  }

}
