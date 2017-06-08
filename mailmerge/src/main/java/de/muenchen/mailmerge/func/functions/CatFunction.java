package de.muenchen.mailmerge.func.functions;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class CatFunction extends MultiFunction
{
  public CatFunction(Collection<Function> subFunction)
  {
    super(subFunction);
  }

  public CatFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  public String getString(Values parameters)
  {
    Iterator<Function> iter = subFunction.iterator();
    StringBuffer res = new StringBuffer();
    while (iter.hasNext())
    {
      Function func = iter.next();
      String str = func.getString(parameters);
      if (str == Function.ERROR)
        return Function.ERROR;
      res.append(str);
    }
    return res.toString();
  }
}