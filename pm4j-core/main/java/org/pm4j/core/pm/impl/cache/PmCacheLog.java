package org.pm4j.core.pm.impl.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pm4j.core.pm.PmObject;
import org.pm4j.core.pm.impl.PmUtil;

public final class PmCacheLog {

  public static final PmCacheLog INSTANCE = new PmCacheLog();

  private static final Log LOG = LogFactory.getLog(PmCacheLog.class);

  private Map<String, Long> pmCacheHitMap = Collections.synchronizedMap(new HashMap<String, Long>());
  private Map<String, Long> pmCacheInitMap = Collections.synchronizedMap(new HashMap<String, Long>());

  public final void logPmCacheHit(PmObject pm, String cacheItem) {
    if (LOG.isTraceEnabled()) {
      doLog(pm, cacheItem, pmCacheHitMap, "hit");
    }
  }

  public final void logPmCacheInit(PmObject pm, String cacheItem) {
    if (LOG.isTraceEnabled()) {
      doLog(pm, cacheItem, pmCacheInitMap, "init");
    }
  }

  private final void doLog(PmObject pm, String cacheItem, Map<String, Long> counterMap, String mapKind) {
    String key = PmUtil.getAbsoluteName(pm) + "-" + cacheItem;
    Long count = counterMap.get(key);
    count = (count == null) ? 1L : ++count;

    counterMap.put(key, count);

    double countSqrt = Math.sqrt(count);
    if ((Math.ceil(countSqrt) - countSqrt) == 0) {
      LOG.trace("Pm cache " + mapKind + "  for: " + key + getHitRatio(key));
    }
  }

  private String getHitRatio(String key) {
    Long inits = pmCacheInitMap.get(key);
    Long hits = pmCacheHitMap.get(key);
    inits = (inits == null) ? 0 : inits;
    hits = (hits == null) ? 0 : hits;

    return " hits/inits=" + hits + "/" + inits;
  }

}
