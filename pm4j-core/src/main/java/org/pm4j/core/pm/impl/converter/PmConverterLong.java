package org.pm4j.core.pm.impl.converter;

/**
 * A number converter for type {@link Long}.
 *
 * @author olaf boede
 */
public class PmConverterLong extends PmConverterNumber<Long> {

  public static final PmConverterLong INSTANCE = new PmConverterLong();

  public PmConverterLong() {
   super(Long.class);
  }
}