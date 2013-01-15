package org.pm4j.common.pageable.querybased;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pm4j.common.query.QueryOptions;
import org.pm4j.common.query.QueryParams;
import org.pm4j.common.util.beanproperty.ReleaseOnPropChangeRef;

/**
 * A proxy service that provides some caching for querys that are already answered.
 * If subsequent queries for one and the same query are reqested, always the result for the first
 * call gets returned.
 * <p>
 * Observes the provided {@link QueryParams}. It resets cached data if the query parameter data
 * get changed.
 *
 * @author olaf boede
 *
 * @param <T_ITEM> type of collection items.
 * @param <T_ID> type of collection item id's.
 */
class CachingPageableQueryService<T_ITEM, T_ID extends Serializable> implements PageableQueryService<T_ITEM, T_ID> {
  private final PageableQueryService<T_ITEM, T_ID> baseService;
  private final CachingPageableQueryService.Cache<T_ITEM, T_ID> cache;

  /**
   * Creates a caching proxy service for the given base service.
   *
   * @param service the service to cache data for.
   */
  public CachingPageableQueryService(PageableQueryService<T_ITEM, T_ID> service) {
    assert service != null;
    this.baseService = service;
    this.cache = new CachingPageableQueryService.Cache<T_ITEM, T_ID>(service);
  }

  @Override
  public T_ID getIdForItem(T_ITEM item) {
    return baseService.getIdForItem(item);
  }

  @Override
  public T_ITEM getItemForId(T_ID id) {
    T_ITEM i = cache.idToPageItemsCache.get(id);
    return (i != null)
            ? i
            : baseService.getItemForId(id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<T_ITEM> getItems(QueryParams query, long startIdx, int pageSize) {
    if (!query.isExecQuery()) {
      return Collections.EMPTY_LIST;
    }

    if ((query == cache.pageCacheQuery.getRef()) &&
		  (startIdx == cache.cachedPageStartIdx) &&
        (pageSize == cache.cachedPageSize)
        ) {
      return cache.pageItemsCache;
    }
    else {
      List<T_ITEM> items = baseService.getItems(query, startIdx, pageSize);
      cache.setPageCache(query, items, startIdx, pageSize);
      return items;
    }
  }

  @Override
  public long getItemCount(QueryParams query) {
    if ((query != cache.itemCountCacheQuery.getRef()) ||
  	  (cache.itemCountCache == -1)) {
 		  cache.itemCountCache = baseService.getItemCount(query);
 		  cache.itemCountCacheQuery.setRef(query);
    }
    return cache.itemCountCache;
  }

  @Override
  public QueryOptions getQueryOptions() {
    return baseService.getQueryOptions();
  }

  /**
   * Provides access to the cache. E.g. for directly re-setting the cache.
   *
   * @return the cache. Never <code>null</code>.
   */
  public Cache<T_ITEM, T_ID> getCache() {
    return cache;
  }

  /**
   * The service behind this caching proxy.
   *
   * @return the backing service.
   */
  public PageableQueryService<T_ITEM, T_ID> getBaseService() {
    return baseService;
  }

  static class Cache<T_ITEM, T_ID extends Serializable> {
    private final PageableQueryService<T_ITEM, T_ID> service;
    private long                                     cachedPageStartIdx = -1;
    private int                                      cachedPageSize = -1;
    private List<T_ITEM>                             pageItemsCache;
    private Map<T_ID, T_ITEM>                        idToPageItemsCache = Collections.emptyMap();
    private final ReleaseOnPropChangeRef<QueryParams> pageCacheQuery;
    private final ReleaseOnPropChangeRef<QueryParams> itemCountCacheQuery;
    /** Cached number of items for the current {@link #itemCountCacheQuery}. */
    private long                                     itemCountCache = -1;

    public Cache(PageableQueryService<T_ITEM, T_ID> service) {
      assert service != null;
      this.service = service;
      this.pageCacheQuery = new ReleaseOnPropChangeRef<QueryParams>(null, QueryParams.PROP_EFFECTIVE_FILTER, QueryParams.PROP_EFFECTIVE_SORT_ORDER) {
        @Override
        protected void onSetRef() {
          clearPageCache();
        }
      };
      this.itemCountCacheQuery = new ReleaseOnPropChangeRef<QueryParams>(null, QueryParams.PROP_EFFECTIVE_FILTER) {
        @Override
        protected void onSetRef() {
          clearItemCountCache();
        }
      };
    }

    public void clearPageCache() {
      cachedPageStartIdx = -1;
      cachedPageSize = -1;
      idToPageItemsCache = Collections.emptyMap();
      pageCacheQuery.setRef(null);
      pageItemsCache = null;
    }

    public void clearItemCountCache() {
      itemCountCache = -1;
      itemCountCacheQuery.setRef(null);
    }

    public void clear() {
      clearPageCache();
      clearItemCountCache();
    }

    public void setPageCache(QueryParams forQuery, List<T_ITEM> pageItemsCache, long startIdx, int pageSize) {
      HashMap<T_ID, T_ITEM> id2Items = new HashMap<T_ID, T_ITEM>();
      for (T_ITEM i : pageItemsCache) {
        T_ID id = service.getIdForItem(i);
        id2Items.put(id, i);
      }
      this.pageCacheQuery.setRef(forQuery);
      this.cachedPageStartIdx = startIdx;
      this.cachedPageSize = pageSize;
      this.pageItemsCache = pageItemsCache;
      this.idToPageItemsCache = id2Items;
    }
  }
}