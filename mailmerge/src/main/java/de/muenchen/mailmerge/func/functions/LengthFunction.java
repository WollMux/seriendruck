package de.muenchen.mailmerge.func.functions;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class LengthFunction extends CatFunction
{
  public LengthFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  public String getString(Values parameters)
  {
    String res = super.getString(parameters);
    if (res == Function.ERROR)
      return Function.ERROR;
    return "" + res.length();
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return false;
  }
}