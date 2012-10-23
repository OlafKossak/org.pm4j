package org.pm4j.core.pm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

import org.pm4j.common.selection.SelectMode;
import org.pm4j.core.pm.PmTable;
import org.pm4j.core.pm.PmTable.RowSelectMode;
import org.pm4j.core.pm.PmTableCol;
import org.pm4j.core.pm.impl.PmTableImpl;

/**
 * Annotation configuration for {@link PmTable} instances.
 *
 * @author olaf boede
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface PmTableCfg2 {

  /**
   * An optional default setting for column sortability.
   * The column may override this default setting.
   *
   * @return <code>true</code> if the columns are sortable by default.<br>
   *         <code>false</code> if the columns is not sortable by default.
   */
  boolean sortable() default false;

  /**
   * Defines the (optional) default sort column.
   * <ul>
   *  <li>Sort by a column using the default 'asc' sort order:  <code>defaultSortCol="myColumnName"</code> </li>
   *  <li>Sort by a column with an explicely defined sort order:  <code>defaultSortCol="myColumnName,desc"</code> </li>
   * </ul>
   *
   * @return
   */
  String defaultSortCol() default "";

  /**
   * Defines a bean comparator that provides the initial table sort order.
   * <p>
   * In difference to {@link #defaultSortCol()} this initial sort order gets not reflected
   * within the {@link PmTableCol#getSortOrderAttr()}.
   * It just defines the initial 'unsorted' state of the table items.
   *
   * @return The defined comparator.
   */
  Class<?> initialBeanSortComparator() default Comparator.class;

  /**
   * Defines the {@link RowSelectMode} to use.
   * <p>
   * This definition may be overridden by a call to {@link PmTableImpl#setRowSelectMode(RowSelectMode)}.
   *
   * @return The row selection mode.
   */
  SelectMode rowSelectMode() default SelectMode.DEFAULT;

  /**
   * Defines the maximum number of rows per page.
   * <p>
   * This definition may be overridden by a call to {@link PmTableImpl#setNumOfPageRows(Integer)}.
   *
   * @return The maximum number of rows per page. Only a positive value has an effect on the table.
   */
  int numOfPageRows() default 0;

}