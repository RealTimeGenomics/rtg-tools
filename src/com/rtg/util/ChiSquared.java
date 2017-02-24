/*
 * Copyright (c) 2017. Real Time Genomics Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.rtg.util;

/**
 * Chi-squared statistical test.
 */
public final class ChiSquared {

  private ChiSquared() {
  }

  private static final double LN2 = Double.longBitsToDouble(0x3FE62E42FEFA39EFL);
  private static final double LNPI = Math.log(Math.PI);
  private static final double TWOPI = 2.0 * Math.PI;
  private static final double SQRT2PI = Math.sqrt(TWOPI);
  private static final double LNSQRT2PI = Math.log(SQRT2PI);
  /* 2^-53, minimal number of bits in mantissa */
  private static final double MACHINE_PRECISION = 1.11022302462515654042E-16;
  /* The natural logarithm of the largest finite double. */
  private static final double MAXLOG = Double.longBitsToDouble(0x40862E42FEFA39EFL);
  private static final double SQRTHALF = Math.sqrt(0.5);

  /**
   * Evaluate a polynomial at a given value. Computes
   * <code>p(x) = (...(((p0 * x) + p1) * x + p2) * x ...) * x + pn</code>.
   * Behaviour is undefined for <code>x = Double.NEGATIVE_INFINITY</code>.
   *
   * @param x point to evaluate polynomial at
   * @param p coefficients of polynomial
   * @return p(x)
   * @exception NullPointerException if <code>p</code> is null
   * @exception ArrayIndexOutOfBoundsException if <code>p</code> has length
   * less than 2.
   */
  static double polyeval(final double x, final double[] p) {
    double a = p[0];
    int i = 1;
    do {
      a *= x;
      a += p[i];
    } while (++i < p.length);
    return a;
  }

  /**
   * Evaluate a polynomial where the leading coefficient is 1. That is,
   * <code>p(x) = (...((x + p1) * x + p2) * x ...) * x + pn</code>.
   * Behaviour is undefined for <code>x = Double.NEGATIVE_INFINITY</code>.
   *
   * @param x point to evaluate polynomial at
   * @param p coefficients of polynomial (excluding leading 1)
   * @return p(x)
   * @exception NullPointerException if <code>p</code> is null
   * @exception ArrayIndexOutOfBoundsException if <code>p</code> is length 0.
   */
  static double p1eval(final double x, final double[] p) {
    double a = x + p[0];
    for (int i = 1; i < p.length; ++i) {
      a *= x;
      a += p[i];
    }
    return a;
  }

  // Arrays used in the computation of lgamma
  private static final double[] LGAMMAP = {
    8.11614167470508450300E-4,
    -5.95061904284301438324E-4,
    7.93650340457716943945E-4,
    -2.77777777730099687205E-3,
    8.33333333333331927722E-2
  };

  private static final double[] LGAMMAN = {
    -1.37825152569120859100E3,
    -3.88016315134637840924E4,
    -3.31612992738871184744E5,
    -1.16237097492762307383E6,
    -1.72173700820839662146E6,
    -8.53555664245765465627E5
  };

  private static final double[] LGAMMAD = {
    -3.51815701436523470549E2,
    -1.70642106651881159223E4,
    -2.20528590553854454839E5,
    -1.13933444367982507207E6,
    -2.53252307177582951285E6,
    -2.01889141433532773231E6
  };

  /** Variable holding the sign of the gamma function after call to <code>lgamma</code>. */
  static int sLGammaSign = 0;

  /**
   * Return the natural logarithm of the absolute value of the gamma function of a double.
   * The sign of the gamma function can be retrieved by calling <code>lgammaSign</code>
   * after the call to <code>lgamma</code>.
   *
   * @param x0 parameter
   * @return <code>ln|gamma(x)|</code>
   */
  public static double lgamma(final double x0) {
    double x = x0;
    if (x < -34) {
      final double q = -x;
      final double w = lgamma(q); // modifies sLGammaSign
      final double p = Math.floor(q);
      if (p == q) {
        return sLGammaSign * Double.POSITIVE_INFINITY;
      }
      final int i = (int) p;
      sLGammaSign = ((i & 1) == 0) ? -1 : 1;
      double z = q - p;
      if (z > 0.5) {
        z = p + 1 - q;
      }
      return LNPI - Math.log(q * Math.sin(Math.PI * z)) - w;
    }
    if (x < 13) {
      double z = 1;
      while (x >= 3) {
        z *= --x;
      }
      while (x < 2) {
        if (x == 0) {
          return Double.POSITIVE_INFINITY;
        }
        z /= x++;
      }
      if (z < 0) {
        sLGammaSign = -1;
        z = -z;
      } else {
        sLGammaSign = 1;
      }
      if (x == 2) {
        return Math.log(z);
      }
      x -= 2;
      final double p = x * polyeval(x, LGAMMAN) / p1eval(x, LGAMMAD);
      return Math.log(z) + p;
    }
    if (x > 2.556348E305) {
      return Double.POSITIVE_INFINITY;
    }
    sLGammaSign = 1;
    double qq = (x - 0.5) * Math.log(x) - x + LNSQRT2PI;
    if (x > 1E8) {
      return qq;
    }
    final double pp = 1 / (x * x);
    if (x >= 1000) {
      qq += ((7.9365079365079365079365E-4 * pp - 2.7777777777777777777778E-3) * pp + 0.0833333333333333333333) / x;
    } else {
      qq += polyeval(pp, LGAMMAP) / x;
    }
    return qq;
  }

  private static final double[] INORMALN0 = {
    -5.99633501014107895267E1,
    9.80010754185999661536E1,
    -5.66762857469070293439E1,
    1.39312609387279679503E1,
    -1.23916583867381258016E0
  };
  private static final double[] INORMALD0 = {
    1.95448858338141759834E0,
    4.67627912898881538453E0,
    8.63602421390890590575E1,
    -2.25462687854119370527E2,
    2.00260212380060660359E2,
    -8.20372256168333339912E1,
    1.59056225126211695515E1,
    -1.18331621121330003142E0
  };
  private static final double[] INORMALN1 = {
    4.05544892305962419923E0,
    3.15251094599893866154E1,
    5.71628192246421288162E1,
    4.40805073893200834700E1,
    1.46849561928858024014E1,
    2.18663306850790267539E0,
    -1.40256079171354495875E-1,
    -3.50424626827848203418E-2,
    -8.57456785154685413611E-4
  };
  private static final double[] INORMALD1 = {
    1.57799883256466749731E1,
    4.53907635128879210584E1,
    4.13172038254672030440E1,
    1.50425385692907503408E1,
    2.50464946208309415979E0,
    -1.42182922854787788574E-1,
    -3.80806407691578277194E-2,
    -9.33259480895457427372E-4
  };
  private static final double[] INORMALN2 = {
    3.23774891776946035970E0,
    6.91522889068984211695E0,
    3.93881025292474443415E0,
    1.33303460815807542389E0,
    2.01485389549179081538E-1,
    1.23716634817820021358E-2,
    3.01581553508235416007E-4,
    2.65806974686737550832E-6,
    6.23974539184983293730E-9
  };
  private static final double[] INORMALD2 = {
    6.02427039364742014255E0,
    3.67983563856160859403E0,
    1.37702099489081330271E0,
    2.16236993594496635890E-1,
    1.34204006088543189037E-2,
    3.28014464682127739104E-4,
    2.89247864745380683936E-6,
    6.79019408009981274425E-9
  };

  private static final double INORMALC0 = Math.exp(-2);
  private static final double INORMALC1 = 1.0 - INORMALC0;

  /**
   * Return the value y such that the area under the normal curve from
   * -infinity to y is equal to x.
   * @param x0 parameter
   * @return the deviation giving area x
   */
  public static double inormal(final double x0) {
    double x = x0;
    if (Double.isNaN(x)) {
      return x;
    } else if (x <= 0.0) {
      return Double.NEGATIVE_INFINITY; // empty region
    } else if (x >= 1.0) {
      return Double.POSITIVE_INFINITY; // entire region
    }
    boolean code = true;
    if (x > INORMALC1) {
      x = 1.0 - x;
      code = false;
    }
    if (x > INORMALC0) {
      x -= 0.5;
      final double x2 = x * x;
      return (x + x * (x2 * polyeval(x2, INORMALN0) / p1eval(x2, INORMALD0))) * SQRT2PI;
    }
    double y = Math.sqrt(-2.0 * Math.log(x));
    final double y0 = y - Math.log(y) / y;
    final double z = 1.0 / y;
    if (y < 8.0) {
      y = z * polyeval(z, INORMALN1) / p1eval(z, INORMALD1);
    } else {
      y = z * polyeval(z, INORMALN2) / p1eval(z, INORMALD2);
    }
    return code ? y - y0 : y0 - y;
  }

  /**
   * Compute the incomplete gamma integral of a and x. This function is defined
   * by <code>incompletegamma(a,x) = (1/\gamma(x))\int_0^x e^{-t}t^{a-1} dt</code>.
   *
   * @param a parameter
   * @param x parameter
   * @return the incomplete gamma function of a and x
   */
  public static double incompletegamma(final double a, final double x) {
    if (x <= 0 || a <= 0) {
      return 0;
    }
    if (x > 1 && x > a) {
      return 1 - incompletegammacomplement(a, x);
    }
    final double ax = a * Math.log(x) - x - lgamma(a);
    if (ax < -MAXLOG) {
      return 0; // underflow
    }
    final double axe = Math.exp(ax);
    // power series
    double r = a;
    double c = 1;
    double ans = 1;
    do {
      ++r;
      c *= x / r;
      ans += c;
    } while (c / ans > MACHINE_PRECISION);
    return ans * axe / a;
  }

  // constants needed for continued fraction scaling
  private static final double IGCBIG = 4.503599627370496E15;
  private static final double IGCBIGINV = 1 / IGCBIG;

  /**
   * Compute the complement of the incomplete gamma integral. That is,
   * <code>incompletegammacomplement(a,x) = 1 - incompletegamma(a,x)</code>.
   *
   * @param a parameter
   * @param x parameter
   * @return <code>1 - incompletegamma(a,x)</code>
   */
  public static double incompletegammacomplement(final double a, final double x) {
    if (x <= 0 || a <= 0) {
      return 1;
    }
    if (x < 1 || x < a) {
      return 1 - incompletegamma(a, x);
    }
    final double ax = a * Math.log(x) - x - lgamma(a);
    if (ax < -MAXLOG) {
      return 0; // underflow
    }
    final double axe = Math.exp(ax);
    // continued fraction
    double y = 1 - a;
    double z = x + y + 1;
    double c = 0;
    double pkm2 = 1;
    double qkm2 = x;
    double pkm1 = x + 1;
    double qkm1 = z * x;
    double ans = pkm1 / qkm1;
    double t;
    do {
      z += 2;
      final double yc = ++y * ++c;
      final double pk = pkm1 * z - pkm2 * yc;
      final double qk = qkm1 * z - qkm2 * yc;
      if (qk != 0) {
        final double r = pk / qk;
        t = Math.abs((ans - r) / r);
        ans = r;
      } else {
        t = 1;
      }
      pkm2 = pkm1;
      pkm1 = pk;
      qkm2 = qkm1;
      qkm1 = qk;
      if (Math.abs(pk) > IGCBIG) {
        pkm2 *= IGCBIGINV;
        pkm1 *= IGCBIGINV;
        qkm2 *= IGCBIGINV;
        qkm1 *= IGCBIGINV;
      }
    } while (t > MACHINE_PRECISION);
    return ans * axe;
  }

  /**
   * Return the chi-squared distribution function for probability <code>p</code>
   * and <code>v</code> degrees of freedom.
   * @param v degrees of freedom, v &gt; 0
   * @param p 0.000002 &lt;= p &lt;= 0.999998
   * @return chi-squared value
   */
  public static double chi(final double v, final double p) {
    if (p < 0.000002 || p > 0.999998) {
      throw new ArithmeticException("p out of bounds in chisquared");
    } else if (v <= 0.0) {
      throw new ArithmeticException("Nonpositive degrees of freedom in chisquared");
    }
    final double xx = 0.5 * v;
    final double c = xx - 1.0;
    final double g = lgamma(xx);
    double ch;
    if (v < -1.24 * Math.log(p)) {
      ch = Math.pow(p * xx * Math.exp(g + xx * LN2), 1.0 / xx);
      if (ch < 0.5E-6) {
        return ch;
      }
    } else if (v > 0.32) {
      final double x = inormal(p);
      final double p1 = 0.222222 / v;
      ch = x * Math.sqrt(p1) + 1.0 - p1;
      ch = ch * ch * ch;
      ch *= v;
      if (ch > 2.2 * v + 6.0) {
        ch = -2.0 * (Math.log(1.0 - p) - c * Math.log(0.5 * ch) + g);
      }
    } else {
      ch = 0.4;
      double q;
      final double a = Math.log(1.0 - p);
      do {
        q = ch;
        final double p1 = 1.0 + ch * (4.67 + ch);
        final double p2 = ch * (6.73 + ch * (6.66 + ch));
        final double t = -0.5 + (4.67 + 2.0 * ch) / p1 - (6.73 + ch * (13.32 + 3.0 * ch)) / p2;
        ch -= (1.0 - Math.exp(a + g + 0.5 * ch + c * LN2) * p2 / p1) / t;
      } while (Math.abs(q / ch - 1.0) > 0.01);
    }
    for (int i = 0; i < 7; ++i) {
      final double q = ch;
      final double p1 = 0.5 * ch;
      final double p2 = p - incompletegamma(xx, p1);
      final double t = p2 * Math.exp(xx * LN2 + g + p1 - c * Math.log(ch));
      final double b = t / ch;
      final double a = 0.5 * t - b * c;
      final double s1 = (210.0 + a * (140.0 + a * (105.0 + a * (84.0 + a * (70.0 + 60.0 * a))))) / 420.0;
      ch += t * (1.0 + .5 * t * s1 - b * c * (s1 - b * (((420.0 + a * (735.0 + a * (966.0 + a * (1141.0 + 1278.0 * a)))) / 2520.0) - b * (((210.0 + a * (462.0 + a * (707.0 + 932.0 * a))) / 2520.0) - b * (((252.0 + a * (672.0 + 1182.0 * a) + c * (294.0 + a * (889.0 + 1740.0 * a))) / 5040.0) - b * (((84.0 + 264.0 * a + c * (175.0 + 606.0 * a)) / 2520.0) - b * ((120.0 + c * (346.0 + 127.0 * c)) / 5040.0)))))));
      if (Math.abs(q / ch - 1.0) > 0.5E-6) {
        return ch;
      }
    }
    return ch;
  }

  private static final double[] ERFCN0 = {
    2.46196981473530512524E-10,
    5.64189564831068821977E-1,
    7.46321056442269912687E0,
    4.86371970985681366614E1,
    1.96520832956077098242E2,
    5.26445194995477358631E2,
    9.34528527171957607540E2,
    1.02755188689515710272E3,
    5.57535335369399327526E2
  };
  private static final double[] ERFCD0 = {
    1.32281951154744992508E1,
    8.67072140885989742329E1,
    3.54937778887819891062E2,
    9.75708501743205489753E2,
    1.82390916687909736289E3,
    2.24633760818710981792E3,
    1.65666309194161350182E3,
    5.57535340817727675546E2
  };
  private static final double[] ERFCN1 = {
    5.64189583547755073984E-1,
    1.27536670759978104416E0,
    5.01905042251180477414E0,
    6.16021097993053585195E0,
    7.40974269950448939160E0,
    2.97886665372100240670E0
  };
  private static final double[] ERFCD1 = {
    2.26052863220117276590E0,
    9.39603524938001434673E0,
    1.20489539808096656605E1,
    1.70814450747565897222E1,
    9.60896809063285878198E0,
    3.36907645100081516050E0
  };
  private static final double[] ERFN = {
    9.60497373987051638749E0,
    9.00260197203842689217E1,
    2.23200534594684319226E3,
    7.00332514112805075473E3,
    5.55923013010394962768E4
  };
  private static final double[] ERFD = {
    3.35617141647503099647E1,
    5.21357949780152679795E2,
    4.59432382970980127987E3,
    2.26290000613890934246E4,
    4.92673942608635921086E4
  };

  /**
   * Compute the complement of the error function, <code>erfc(x) = 1.0 - erf(x)</code>.
   * @param x parameter
   * @return <code>erfc(x)</code>
   */
  public static double erfc(final double x) {
    final double ax = Math.abs(x);
    if (ax < 1.0) {
      return 1.0 - erf(x);
    }
    double z = -x * x;
    if (z < -MAXLOG) {
      return x < 0 ? 2.0 : 0.0;
    }
    z = Math.exp(z);
    final double p;
    final double q;
    if (ax < 8.0) {
      p = polyeval(ax, ERFCN0);
      q = p1eval(ax, ERFCD0);
    } else {
      p = polyeval(ax, ERFCN1);
      q = p1eval(ax, ERFCD1);
    }
    double y = (z * p) / q;
    if (x < 0) {
      y = 2.0 - y;
    }
    return y == 0.0 ? (x < 0 ? 2.0 : 0.0) : y;
  }

  /**
   * Compute the error function, erf(x), of a double x where
   * <code> erf(x) = {2/sqrt(pi)}\int_0^x e^(-t^2) dt. </code> The magnitude of
   * x is limited to Math.log(Double.MAX_VALUE).
   * @param x parameter
   * @return erf(x)
   */
  public static double erf(final double x) {
    if (Math.abs(x) > 1.0) {
      return 1.0 - erfc(x);
    }
    final double z = x * x;
    return x * polyeval(z, ERFN) / p1eval(z, ERFD);
  }

  /**
   * Return the area under the normal distribution integrated from minus infinity to x.
   * @param x0 parameter
   * @return area from -infinity to x
   */
  public static double normal(final double x0) {
    final double x = x0 * SQRTHALF;
    final double z = Math.abs(x);
    if (z < SQRTHALF) {
      return 0.5 + 0.5 * erf(x);
    }
    final double y = 0.5 * erfc(z);
    return x > 0.0 ? 1.0 - y : y;
  }
}
