package org.pm4j.core.pm.pageable2;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.pm4j.common.pageable.ModificationHandler;
import org.pm4j.common.pageable.Modifications;
import org.pm4j.common.pageable.PageableCollection2;
import org.pm4j.common.pageable.inmem.PageableInMemCollectionImpl;
import org.pm4j.common.query.QueryOptions;
import org.pm4j.common.query.QueryParams;
import org.pm4j.common.selection.Selection;
import org.pm4j.common.selection.SelectionHandler;
import org.pm4j.core.pm.PmBean;
import org.pm4j.core.pm.PmDataInput;
import org.pm4j.core.pm.PmEvent;
import org.pm4j.core.pm.PmEventListener;
import org.pm4j.core.pm.PmObject;
import org.pm4j.core.pm.api.PmEventApi;
import org.pm4j.core.pm.api.PmFactoryApi;
import org.pm4j.core.pm.api.PmMessageUtil;
import org.pm4j.core.pm.impl.BeanPmCacheUtil;
import org.pm4j.core.pm.pageable.PageableCollection;
import org.pm4j.core.pm.pageable.PageableListImpl;

/**
 * A {@link PageableCollection2} instance that provides {@link PmBean} instances in
 * front of a {@link PageableCollection2} container that handles the corresponding
 * bean instances.
 *
 * @author olaf boede
 *
 * @param <T_PM>
 *          The kind of {@link PmBean} provided by this class.
 * @param <T_BEAN>
 *          The kind of corresponding bean, handled by the backing
 *          {@link PageableCollection} instance.
 */
public class PageablePmBeanCollection<T_PM extends PmBean<T_BEAN>, T_BEAN> implements PageableCollection2<T_PM> {

  /** The collection type specific selection handler. */
  private SelectionHandlerWithPmFactory<T_PM, T_BEAN> selectionHandler;
  private final PmBeanCollectionModificationHandler   modificationHandler;

  private final PmObject                              pmCtxt;
  private final PageableCollection2<T_BEAN>           beanCollection;
  /** Type of item PM's the change handler should observe changes for. */
  private final Class<?>                              itemPmClass;


  /**
   * Creates a collection backed by the given {@link PageableCollection} of beans.
   *
   * @param pmCtxt
   *          The PM context used to create the PM's for the bean items.
   * @param pageableBeanCollection
   *          The collection of beans to represent by this collection of bean-PM's.
   */
  public PageablePmBeanCollection(PmObject pmCtxt, Class<?> itemPmClass, PageableCollection2<T_BEAN> pageableBeanCollection) {
    assert pmCtxt != null;
    assert pageableBeanCollection != null;
    assert itemPmClass != null;

    this.pmCtxt = pmCtxt;
    this.beanCollection = pageableBeanCollection;
    this.itemPmClass = itemPmClass;
    this.selectionHandler = new SelectionHandlerWithPmFactory<T_PM, T_BEAN>(pmCtxt, pageableBeanCollection.getSelectionHandler());
    this.modificationHandler = new PmBeanCollectionModificationHandler();
  }

  /**
   * Creates a collection backed by a {@link PageableListImpl}.
   *
   * @param pmCtxt
   *          The PM context used to create the PM's for the bean items.
   * @param beans
   *          The set of beans to handle.
   * @param queryOptions
   *          the optional query options.
   */
  public PageablePmBeanCollection(PmObject pmCtxt, Class<?> itemPmClass, Collection<T_BEAN> beans, QueryOptions queryOptions) {
	  this(pmCtxt, itemPmClass, beans, queryOptions, null);
  }

  /**
   * Creates a collection backed by a {@link PageableListImpl}.
   *
   * @param pmCtxt
   *          The PM context used to create the PM's for the bean items.
   * @param beans
   *          The set of beans to handle.
   * @param queryOptions
   *          the optional query options.
   * @param query
   *          the used query. Is optional.
   */
  public PageablePmBeanCollection(PmObject pmCtxt, Class<?> itemPmClass, Collection<T_BEAN> beans, QueryOptions queryOptions, QueryParams query) {
    this(pmCtxt, itemPmClass,
         new PageableInMemCollectionImpl<T_BEAN>(
             new InMemPmQueryEvaluator<T_BEAN>(pmCtxt),
             beans,
             queryOptions,
             query)
        );
  }

  /**
   * Creates a collection backed by a {@link PageableInMemCollectionImpl} with no query constraints.
   *
   * @param pmCtxt
   *          The PM context used to create the PM's for the bean items.
   * @param beans
   *          The set of beans to handle.
   */
  public PageablePmBeanCollection(PmObject pmCtxt, Class<?> itemPmClass, Collection<T_BEAN> beans) {
    this(pmCtxt, itemPmClass, beans, new QueryOptions(), null);
  }

  @Override
  public QueryParams getQueryParams() {
    return beanCollection.getQueryParams();
  }

  @Override
  public QueryOptions getQueryOptions() {
    return beanCollection.getQueryOptions();
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<T_PM> getItemsOnPage() {
    return (List<T_PM>) PmFactoryApi.getPmListForBeans(pmCtxt, beanCollection.getItemsOnPage(), false);
  }

  @Override
  public int getPageSize() {
    return beanCollection.getPageSize();
  }

  @Override
  public void setPageSize(int newSize) {
    beanCollection.setPageSize(newSize);
  }

  @Override
  public long getPageIdx() {
    return beanCollection.getPageIdx();
  }
  @Override
  public int getCurrentPageIdx() {
    return beanCollection.getCurrentPageIdx();
  }

  @Override
  public void setCurrentPageIdx(int pageIdx) {
    beanCollection.setCurrentPageIdx(pageIdx);
  }
  @Override
  public void setPageIdx(long pageIdx) {
    beanCollection.setPageIdx(pageIdx);
  }

  @Override
  public long getNumOfItems() {
    return beanCollection.getNumOfItems();
  }

  @Override
  public Iterator<T_PM> iterator() {
    final Iterator<T_BEAN> beanIter = beanCollection.iterator();
    return new Iterator<T_PM>() {
      @Override
      public boolean hasNext() {
        return beanIter.hasNext();
      }
      @Override
      public T_PM next() {
        T_BEAN b = beanIter.next();
        return b != null
            ? (T_PM) PmFactoryApi.<T_BEAN, T_PM>getPmForBean(pmCtxt, b)
            : null;
      }
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public SelectionHandler<T_PM> getSelectionHandler() {
    return selectionHandler;
  }

  @Override
  public Selection<T_PM> getSelection() {
    return selectionHandler.getSelection();
  }

  @Override
  public void clearCaches() {
    beanCollection.clearCaches();
  }

  @Override
  public ModificationHandler<T_PM> getModificationHandler() {
    return modificationHandler;
  }

  @Override
  public Modifications<T_PM> getModifications() {
    return modificationHandler.getModifications();
  }

  /**
   * Provides the pageable collection of beans behind this collection of bean-PMs.
   *
   * @return the bean collection.
   */
  public PageableCollection2<T_BEAN> getBeanCollection() {
    return beanCollection;
  }

  /**
   * Defines a use case specific selection handler.
   *
   * @param selectionHandler
   */
  protected void setSelectionHandler(SelectionHandlerWithPmFactory<T_PM, T_BEAN> selectionHandler) {
    this.selectionHandler = selectionHandler;
  }

  /**
   * Delegates modification handling to the backing pageable bean collection.
   */
  class PmBeanCollectionModificationHandler implements ModificationHandler<T_PM> {

    /** The modification register. */
    private final PmBeanModifications modifications = new PmBeanModifications();

    /** Listens for changed state changes in the subtree and updates the changedRows accordingly. */
    private final PmChangeListener itemHierarchyChangeListener = new PmChangeListener();

    public PmBeanCollectionModificationHandler() {
      PmEventApi.addHierarchyListener(pmCtxt, PmEvent.VALUE_CHANGED_STATE_CHANGE, itemHierarchyChangeListener);
    }

    @Override
    public void addItem(T_PM item) {
      getBeanCollectionModificationHandler().addItem(item.getPmBean());
    }

    @Override
    public void updateItem(T_PM item, boolean isUpdated) {
      getBeanCollectionModificationHandler().updateItem(item.getPmBean(), isUpdated);
    }

    // TODO olaf: ensure that the cleanup code also gets called when the bean modification handler gets used.
    @Override
    public boolean removeSelectedItems() {
      // clear all messages and references to the set of items to delete.
      Selection<T_PM> selection = getSelectionHandler().getSelection();
      for (T_PM p : getItemsOnPage()) {
        if (selection.isSelected(p)) {
          PmMessageUtil.clearSubTreeMessages(p);
          BeanPmCacheUtil.removeBeanPm(pmCtxt, p);
        }
      }
      for (T_PM p : getModifications().getUpdatedItems()) {
        if (selection.isSelected(p)) {
          PmMessageUtil.clearSubTreeMessages(p);
          BeanPmCacheUtil.removeBeanPm(pmCtxt, p);
        }
      }

      return getBeanCollectionModificationHandler().removeSelectedItems();
    }

    private ModificationHandler<T_BEAN> getBeanCollectionModificationHandler() {
      ModificationHandler<T_BEAN> mh = beanCollection.getModificationHandler();
      if (mh == null) {
        throw new RuntimeException("Pageable bean collection without a modification handler can't handle modifications." + beanCollection);
      }
      return mh;
    }

    @Override
    public void clearRegisteredModifications() {
      ModificationHandler<T_BEAN> mh = beanCollection.getModificationHandler();
      if (mh != null) {
        mh.clearRegisteredModifications();
      }
    }

    @Override
    public Modifications<T_PM> getModifications() {
      return modifications;
    }

    /** Provides PM type specific modifications based on the backing bean collection. */
    class PmBeanModifications implements Modifications<T_PM> {

      @Override
      public boolean isModified() {
        return getBeanCollectionModificationHandler().getModifications().isModified();
      }

      @SuppressWarnings("unchecked")
      @Override
      public Collection<T_PM> getAddedItems() {
        Collection<T_BEAN> beans = getBeanCollectionModificationHandler().getModifications().getAddedItems();
        return (Collection<T_PM>) PmFactoryApi.getPmListForBeans(pmCtxt, beans, false);
      }

      @SuppressWarnings("unchecked")
      @Override
      public Collection<T_PM> getUpdatedItems() {
        Collection<T_BEAN> beans = getBeanCollectionModificationHandler().getModifications().getUpdatedItems();
        return (Collection<T_PM>) PmFactoryApi.getPmListForBeans(pmCtxt, beans, false);
      }

      @Override
      public Selection<T_PM> getRemovedItems() {
        return new PmSelection<T_PM, T_BEAN>(pmCtxt, getBeanCollectionModificationHandler().getModifications().getRemovedItems());
      }
    };

    /** Listens for changed state changes in the subtree and updates the registered changes accordingly. */
    private class PmChangeListener implements PmEventListener {
      @SuppressWarnings("unchecked")
      @Override
      public void handleEvent(PmEvent event) {
        PmDataInput itemPm = findChildItemToObserve(event.pm);
        if (itemPm != null) {
          modificationHandler.updateItem((T_PM) itemPm, itemPm.isPmValueChanged());
        }
      }

      protected PmDataInput findChildItemToObserve(PmObject changedItem) {
        if (changedItem == pmCtxt) {
          return null;
        }

        PmObject p = changedItem;
        do {
          if (p.getPmParent() == pmCtxt) {
            // Only PM's of the considered type are considered (e.g. row-PM type in case of tables).
            if (itemPmClass.isAssignableFrom(p.getClass())) {
              return (PmDataInput)p;
            }
            else {
              return null;
            }
          }
          p = p.getPmParent();
        }
        while (p != null);

        // the changed item is not a child of the observed PM.
        return null;
      }
    }

  }

}
