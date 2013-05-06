package org.pm4j.core.deprecated;

import org.pm4j.core.pm.PmAttr;
import org.pm4j.core.pm.PmObject;
import org.pm4j.core.pm.impl.PmUtil;

/**
 * A helper that may be used to prevent lazy load issues.
 * <p>
 * Calls all external getter methods within the PM tree.
 * 
 * @deprecated is a workaround for a very very old Hibernate lazy load issue. Should no longer be required. 
 */
public class PmVisitorTouchAll extends PmVisitorAdapter {

  private boolean touchInvisibleItems;

  /**
   * An instance that does not touch invisible items.
   */
  public static final PmVisitorTouchAll DEFAULT_INSTANCE = new PmVisitorTouchAll();

  public PmVisitorTouchAll() {
    this(false);
  }

  public PmVisitorTouchAll(boolean touchInvisibleItems) {
    this.touchInvisibleItems = touchInvisibleItems;
  }

  @Override
  protected void onVisit(PmObject pm) {
    if (touchInvisibleItems || pm.isPmVisible()) {
      pm.getPmTitle();
      pm.getPmTooltip();
      pm.getPmIconPath();
      for (PmObject p : PmUtil.getPmChildren(pm)) {
        p.accept(this);
      }
    }
  }

  @Override
  public void visit(PmAttr<?> attr) {
    super.visit(attr);
    attr.getValue();
  }

}