package org.pm4j.core.pm.impl.converter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.pm4j.core.pm.PmAttr;
import org.pm4j.core.pm.PmAttrDouble;

public class PmConverterDouble extends PmConverterNumber<Double> {

  public static final PmConverterDouble INSTANCE = new PmConverterDouble();

  public PmConverterDouble() {
    super(Double.class);
  }

  @Override
  protected NumberFormat getNumberFormat(Locale locale, String formatString, PmAttr<?> pmAttr) {
    DecimalFormat decimalFormat = new DecimalFormat(formatString, new DecimalFormatSymbols(locale));
    if(pmAttr instanceof PmAttrDouble) {
      PmAttrDouble pmAttrDouble = (PmAttrDouble) pmAttr;
      decimalFormat.setRoundingMode(pmAttrDouble.getStringConversionRoundingMode());
    }
    return decimalFormat;
  }
}