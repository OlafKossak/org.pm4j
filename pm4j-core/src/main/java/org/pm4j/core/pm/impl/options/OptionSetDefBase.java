package org.pm4j.core.pm.impl.options;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.pm4j.common.util.collection.IterableUtil;
import org.pm4j.core.exception.PmRuntimeException;
import org.pm4j.core.pm.PmElement;
import org.pm4j.core.pm.PmOption;
import org.pm4j.core.pm.PmOptionSet;
import org.pm4j.core.pm.annotation.PmOptionCfg;
import org.pm4j.core.pm.annotation.PmOptionCfg.NullOption;
import org.pm4j.core.pm.impl.PmAttrBase;
import org.pm4j.core.pm.impl.PmUtil;
import org.pm4j.core.pm.impl.pathresolver.ExpressionPathResolver;
import org.pm4j.core.pm.impl.pathresolver.PathComparatorFactory;
import org.pm4j.core.pm.impl.pathresolver.PathResolver;
import org.pm4j.core.pm.impl.pathresolver.PmExpressionPathResolver;

/**
 * An algorithms that provides options for attribute values based on
 * the annotation {@link PmOptionCfg}.
 *
 * @author olaf boede
 */
public abstract class OptionSetDefBase<T_ATTR extends PmAttrBase<?,?>> implements PmOptionSetDef<T_ATTR> {

  protected final PathResolver optionsPath;
  protected final Method getOptionValuesMethod;
  protected final PathResolver idPath;
  protected final PathResolver titlePath;
  protected final PathResolver valuePath;

  protected final NullOption nullOption;
  protected final String nullOptionTitleResKey;
  protected final PathComparatorFactory sortComparatorFactory;


  public OptionSetDefBase(PmOptionCfg cfg, Method getOptionValuesMethod) {
    this.optionsPath = StringUtils.isNotBlank(cfg.values())
        ? PmExpressionPathResolver.parse(cfg.values(), true)
        : null;
    this.getOptionValuesMethod = getOptionValuesMethod;
    this.idPath = ExpressionPathResolver.parse(cfg.id());
    this.titlePath = ExpressionPathResolver.parse(cfg.title());
    this.valuePath = ExpressionPathResolver.parse(cfg.value());
    this.nullOption = cfg.nullOption();
    this.nullOptionTitleResKey = StringUtils.defaultIfEmpty(cfg.nullOptionResKey(), null);
    this.sortComparatorFactory = PmOptionCfg.NO_SORT_SPEC.equals(cfg.sortBy())
                ? null
                : PathComparatorFactory.parse(cfg.sortBy());
  }

  @Override
  public PmOptionSet makeOptions(T_ATTR forAttr) {
    List<PmOption> list;

    Object o = null;

    if (getOptionValuesMethod != null) {
      try {
        o = getOptionValuesMethod.invoke(forAttr);
      } catch (Exception e) {
        throw new PmRuntimeException(forAttr,
            "Failed to execute the method that provides the option values.", e);
      }
    }
    else {
      o = optionsPath != null
        ? optionsPath.getValue(PmUtil.getPmParentOfType(forAttr, PmElement.class))
        : getOptionValues(forAttr);
  }

    if (o == null) {
      list = Collections.emptyList();
    }
    else if (o instanceof Collection<?>) {
      list = makeOptions(forAttr, (Collection<?>)o);
    }
    else if (o instanceof Object[]) {
      list = makeOptions(forAttr, Arrays.asList((Object[]) o));
    }
    else if (o instanceof Iterable<?>) {
      list = makeOptions(forAttr, IterableUtil.shallowCopy((Iterable<?>)o));
    }
    else if (o instanceof Iterator<?>) {
      list = makeOptions(forAttr, IterableUtil.shallowCopy((Iterator<?>)o));
    }
    else {
      throw new PmRuntimeException(forAttr,
          "The options path does not reference a collection, array, iterable or iterator. Found type: " +
          o.getClass());
    }

    return new PmOptionSetImpl.WithIdMap(list);
  }

  @Override
  public String getNullOptionTitle(T_ATTR forAttr) {
    return PmOptionSetUtil.getNullOptionTitle(forAttr, nullOptionTitleResKey);
  }

  /**
   * Is only called if no {@link #optionsPath} is specified.
   */
  protected Iterable<?> getOptionValues(T_ATTR forAttr) {
    return forAttr.getOptionValues();
  }


  protected abstract PmOption makeOption(T_ATTR forAttr, Object o);

  private List<PmOption> makeOptions(T_ATTR forAttr, Collection<?> objects) {
    if (objects == null || objects.isEmpty()) {
      return Collections.emptyList();
    }
    else {
      List<PmOption> list = new ArrayList<PmOption>();

      for (Object o : objects) {
        list.add(makeOption(forAttr, o));
      }

      if (sortComparatorFactory != null) {
        Collections.sort(list, sortComparatorFactory.getComparator(forAttr));
      }

      // the null option will be added after sorting to prevent
      // sort problems with the null-option.
      if (shouldMakeNullOption(forAttr)) {
        String title = getNullOptionTitle(forAttr);

        List<PmOption> l = new ArrayList<PmOption>(list.size()+1);
        l.add(new PmOptionImpl(null, StringUtils.defaultString(title)));
        l.addAll(list);
        list = l;
      }

      return list;
    }
  }

  protected boolean shouldMakeNullOption(PmAttrBase<?,?> forAttr) {
    NullOption nopt = (nullOption == NullOption.DEFAULT)
                      ? forAttr.getNullOptionDefault()
                      : nullOption;

    return nopt == NullOption.YES ||
            (nopt == NullOption.FOR_OPTIONAL_ATTR &&
             ! forAttr.isRequired()) ||
             forAttr.getValue() == null;
// TODO olaf:
//                      switch (nopt) {
//      case YES: return true;
//      case NO: return forAttr.getValue() != null;
//      case DEFAULT: // fall through
//      case FOR_OPTIONAL_ATTR: return (!forAttr.isRequired()) && forAttr.getValue() != null;
//      default: throw new PmRuntimeException(forAttr, "Unknown enum attribute value " + nopt);
//    }
  }


}
