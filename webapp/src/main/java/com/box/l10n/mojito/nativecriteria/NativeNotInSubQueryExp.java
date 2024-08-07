package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp;

public class NativeNotInSubQueryExp implements NativeExp {
  private final String columnName;

  private final NativeExp nativeExp;

  public NativeNotInSubQueryExp(String columnName, NativeExp nativeExp) {
    this.columnName = columnName;
    this.nativeExp = nativeExp;
  }

  @Override
  public String toSQL() {
    return this.columnName + " NOT IN (" + this.nativeExp.toSQL() + ")";
  }

  @Override
  public void setValues(NativeQuery query) {
    this.nativeExp.setValues(query);
  }
}
