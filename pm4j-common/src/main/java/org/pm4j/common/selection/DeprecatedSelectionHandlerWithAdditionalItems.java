package org.pm4j.common.selection;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pm4j.common.pageable.PageableCollection2;

/**
 * Handles selections for a {@link PageableCollection2} with transient additional items.
 *
 * @author olaf boede
 *
 * @param <T_ITEM>
 * @deprecated please use {@link SelectionHandlerWithAdditionalItems}
 */
public class DeprecatedSelectionHandlerWithAdditionalItems<T_ITEM> extends SelectionHandlerBase<T_ITEM> {
  private static final Log                   LOG              = LogFactory.getLog(DeprecatedSelectionHandlerWithAdditionalItems.class);

  private SelectionWithAdditionalItems<T_ITEM> emptySelection = new SelectionWithAdditionalItems<T_ITEM>(
                                                                 EmptySelection.<T_ITEM> getEmptySelection(), null);

  private final PageableCollection2<T_ITEM>  baseCollection;
  private final List<T_ITEM>                 additionalItems;

  /** The set of currently selected items. */
  private SelectionWithAdditionalItems<T_ITEM> selection      = emptySelection;

  /**
   * @param baseCollection
   *          the backing collection to handle selections for.
   * @param transientItems
   *          a reference to the set of additional transient items to consider.
   */
  public DeprecatedSelectionHandlerWithAdditionalItems(PageableCollection2<T_ITEM> baseCollection, List<T_ITEM> transientItems) {
    assert baseCollection != null;
    assert transientItems != null;

    this.baseCollection = baseCollection;
    this.additionalItems = transientItems;
  }

  @Override
  public void setSelectMode(SelectMode selectMode) {
    super.setSelectMode(selectMode);
    baseCollection.getSelectionHandler().setSelectMode(selectMode);
  }

  // TODO olaf: will be simplified and reduced to a single selection change event when the 'addition item'
  // case gets handled by the query modification/selection handler too.
  @Override
  public boolean select(boolean select, T_ITEM item) {
    // FIXME olaf: get rid of this stack level to get less maintainence overhead...

    SelectionHandler<T_ITEM> baseSelectionHandler = baseCollection.getSelectionHandler();
    Selection<T_ITEM> oldBaseSelection = selection.getBaseSelection();
    boolean success = false;

    if (additionalItems.contains(item)) {
      Collection<T_ITEM> transientItemSelection = new ArrayList<T_ITEM>(selection.getAdditionalSelectedItems());
      Selection<T_ITEM> baseSelection = oldBaseSelection;
      if (select) {
        if (getSelectMode() == SelectMode.SINGLE) {
          // the selection switches to the transient item. All other selections need to be removed.
          if (!SelectionHandlerUtil.selectAllInSameForceMode(this, baseSelectionHandler, false)) {
            return false;
          }
          baseSelection = baseSelectionHandler.getSelection();
          transientItemSelection.clear();
        }
        transientItemSelection.add(item);
      } else {
        transientItemSelection.remove(item);
      }

      SelectionWithAdditionalItems<T_ITEM> newSelection = new SelectionWithAdditionalItems<T_ITEM>(
            baseSelection,
            transientItemSelection);
      success = setSelection(newSelection);
    } else {
      if (!SelectionHandlerUtil.selectInSameForceMode(this, baseSelectionHandler, select, item)) {
        return false;
      }

      SelectionWithAdditionalItems<T_ITEM> newSelection = new SelectionWithAdditionalItems<T_ITEM>(
          baseSelectionHandler.getSelection(),
          getSelectMode() == SelectMode.SINGLE
              ? null
              : selection.getAdditionalSelectedItems());
      success = setSelection(newSelection);
    }

    if (! success) {
      SelectionHandlerUtil.forceSetSelection(baseSelectionHandler, oldBaseSelection);
    }

    return success;
  }

  @Override
  public boolean select(boolean select, Iterable<T_ITEM> items) {
    SelectionHandler<T_ITEM> baseSelectionHandler = baseCollection.getSelectionHandler();
    List<T_ITEM> newTransientItems = new ArrayList<T_ITEM>();
    List<T_ITEM> newBaseItems = new ArrayList<T_ITEM>();

    for (T_ITEM i : items) {
      if (additionalItems.contains(i)) {
        newTransientItems.add(i);
      } else {
        newBaseItems.add(i);
      }
    }

    // try to change the base collection selection.
    // if that fails, we simply can skip this operation without side effects.
    if (!newBaseItems.isEmpty()) {
      if (!SelectionHandlerUtil.selectInSameForceMode(this, baseSelectionHandler, select, newBaseItems)) {
        return false;
      }
    }

    Collection<T_ITEM> selectedTransientItems = new HashSet<T_ITEM>(selection.getAdditionalSelectedItems());
    for (T_ITEM i : newTransientItems) {
      if (select) {
        selectedTransientItems.add(i);
      } else {
        selectedTransientItems.remove(i);
      }
    }
    SelectionWithAdditionalItems<T_ITEM> newSelection = new SelectionWithAdditionalItems<T_ITEM>(
        baseSelectionHandler.getSelection(),
        selectedTransientItems);
    // FIXME olaf: a veto leaves the base selection in a wrong state!
    return setSelection(newSelection);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean selectAll(boolean select) {
    SelectionHandler<T_ITEM> baseSelectionHandler = baseCollection.getSelectionHandler();
    if (!SelectionHandlerUtil.selectAllInSameForceMode(this, baseSelectionHandler, select)) {
      return false;
    }
    SelectionWithAdditionalItems<T_ITEM> newSelection = new SelectionWithAdditionalItems<T_ITEM>(
        baseSelectionHandler.getSelection(),
        select
            ? additionalItems
            : Collections.EMPTY_LIST);

    // FIXME olaf: a veto leaves the base selection in a wrong state!
    return setSelection(newSelection);
  }

  @Override
  public boolean invertSelection() {
    if (getSelectMode() != SelectMode.MULTI) {
      throw new RuntimeException("Invert selection is not supported for select mode: " + getSelectMode());
    }

    SelectionHandler<T_ITEM> baseSelectionHandler = baseCollection.getSelectionHandler();
    if (! SelectionHandlerUtil.invertSelectionInSameForceMode(this, baseSelectionHandler)) {
      return false;
    }

    Collection<T_ITEM> transientSelectedItems = new HashSet<T_ITEM>(additionalItems);
    transientSelectedItems.removeAll(selection.getAdditionalSelectedItems());
    SelectionWithAdditionalItems<T_ITEM> newSel = new SelectionWithAdditionalItems<T_ITEM>(
          baseSelectionHandler.getSelection(),
          transientSelectedItems);

    if (!setSelection(newSel)) {
      SelectionHandlerUtil.forceInvertSelection(baseSelectionHandler);
      return false;
    } else {
      return true;
    }
  }

  @Override
  public Selection<T_ITEM> getSelection() {
    ensureSelectionState();
    return selection;
  }

  @Override
  public boolean setSelection(Selection<T_ITEM> selection) {
    Selection<T_ITEM> oldSelection = this.selection;
    Selection<T_ITEM> newSelection = selection;

    // check for noop:
    if (oldSelection.getSize() == 0 &&
        newSelection.getSize() == 0) {
    	return true;
    }

    try {
      fireVetoableChange(PROP_SELECTION, oldSelection, newSelection);
      this.selection = (SelectionWithAdditionalItems<T_ITEM>) newSelection;
      firePropertyChange(PROP_SELECTION, oldSelection, newSelection);
      return true;
    } catch (PropertyVetoException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Selection change rejected because of a property change veto. " + e.getMessage());
      }
      return false;
    }
  }

}