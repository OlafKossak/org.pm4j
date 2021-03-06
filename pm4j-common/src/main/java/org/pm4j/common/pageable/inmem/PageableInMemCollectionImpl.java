package org.pm4j.common.pageable.inmem;

import java.util.ArrayList;
import java.util.Collection;

import org.pm4j.common.query.QueryOptions;
import org.pm4j.common.query.QueryParams;
import org.pm4j.common.query.inmem.InMemQueryEvaluator;

/**
 * A simple in memory implementation that contains a collection of beans.
 *
 * @param <T_ITEM> the used bean type
 *
 * @author olaf boede
 */
public class PageableInMemCollectionImpl<T_ITEM> extends PageableInMemCollectionBase<T_ITEM> {

  /** Contains the not filtered set of items in their original sort order. */
  private Collection<T_ITEM>             backingCollection;

  public PageableInMemCollectionImpl(Collection<T_ITEM> objects) {
    super(null, null);
    backingCollection = objects != null
        ? objects
        : new ArrayList<T_ITEM>();
  }

  public PageableInMemCollectionImpl(InMemQueryEvaluator<T_ITEM> inMemQueryEvaluator, Collection<T_ITEM> objects, QueryOptions queryOptions, QueryParams query) {
    super(inMemQueryEvaluator, queryOptions, query);
    backingCollection = objects != null
        ? objects
        : new ArrayList<T_ITEM>();
  }

  @Override
  public Collection<T_ITEM> getBackingCollectionImpl() {
    return backingCollection;
  }

}
