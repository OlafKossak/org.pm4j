package org.pm4j.common.pageable.querybased;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.pm4j.common.pageable.PageableCollection2;
import org.pm4j.common.pageable.PageableCollectionWithMultiFieldIdTestBase;
import org.pm4j.common.query.QueryAttrMulti;
import org.pm4j.common.query.QueryAttr;
import org.pm4j.common.query.CompOpStringStartsWith;
import org.pm4j.common.query.FilterCompareDefinition;
import org.pm4j.common.query.QueryOptions;
import org.pm4j.common.query.QueryParams;
import org.pm4j.common.query.SortOrder;
import org.pm4j.common.query.inmem.InMemQueryEvaluator;
import org.pm4j.common.query.inmem.InMemSortOrder;
import org.pm4j.common.util.CompareUtil;
import org.pm4j.common.util.collection.MultiObjectValue;

public class PageableQueryCollectionWithMultiFieldIdTest extends PageableCollectionWithMultiFieldIdTestBase<PageableCollectionWithMultiFieldIdTestBase.Bean> {

  TestService service = new TestService();

  @Override
  protected PageableCollection2<Bean> makePageableCollection(String... strings) {
    int counter = 1;
    for (String s : strings) {
      service.addBean(new Bean(++counter, counter, s));
    }

    return new PageableQueryCollection<Bean, MultiObjectValue>(service);
  }

  @Override
  protected SortOrder getOrderByName() {
    return service.getQueryOptions().getSortOrder("name");
  }

  @Override
  protected Bean createItem(String name) {
    return new Bean(name);
  }

  // --- A fake service implementation that does the job just in memory. ---

  static class TestService implements PageableQueryService<Bean, MultiObjectValue> {

    private Map<MultiObjectValue, Bean> idToBeanMap = new LinkedHashMap<MultiObjectValue, Bean>();

    @Override
    public MultiObjectValue getIdForItem(Bean item) {
      return new MultiObjectValue(item.id1, item.id2);
    }

    @Override
    public Bean getItemForId(MultiObjectValue id) {
      return idToBeanMap.get(id);
    }

    @Override
    public List<Bean> getItems(QueryParams query, long startIdx, int pageSize) {
      if (startIdx >= idToBeanMap.size()) {
        return Collections.emptyList();
      }

      List<Bean> allQueryResultItems = getQueryResult(query);
      int endIdx = Math.min((int)startIdx + pageSize, allQueryResultItems.size());

      List<Bean> beanList = allQueryResultItems.subList((int)startIdx, endIdx);
      return beanList;
    }

    @Override
    public long getItemCount(QueryParams query) {
      return getQueryResult(query).size();
    }

    public void addBean(Bean b) {
      idToBeanMap.put(getIdForItem(b), b);
    }

    // some in memory fakes ...
    @Override
    public QueryOptions getQueryOptions() {
      QueryOptions options = new QueryOptions();
      options.setIdAttribute(
          new QueryAttrMulti("identity (pseudo field)")
            .addPart("id1", Integer.class)
            .addPart("id2", Integer.class));

      QueryAttr nameAttr = new QueryAttr("name", String.class);

      options.addSortOrder("name", new InMemSortOrder(new Comparator<Bean>() {
        @Override
        public int compare(Bean o1, Bean o2) {
          return CompareUtil.compare(o1.name, o2.name);
        }
      }));

      options.addFilterCompareDefinition(new FilterCompareDefinition(nameAttr, new CompOpStringStartsWith()));

      return options;
    }

    private List<Bean> getQueryResult(QueryParams query) {
      InMemQueryEvaluator<Bean> evalCtxt = new InMemQueryEvaluator<Bean>();
      List<Bean> beans = evalCtxt.sort(idToBeanMap.values(), query.getEffectiveSortOrder());

      if (query.getFilterExpression() != null) {
        beans = evalCtxt.evaluateSubSet(beans, query.getFilterExpression());
      }

      return beans;
    }
  }
}
