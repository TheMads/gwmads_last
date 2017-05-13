package ru.l2gw.gameserver.model.variation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: rage
 * @date: 18.10.11 21:18
 */
public class VariationGroup
{
	private static final Log _log = LogFactory.getLog("variation");

	private final int chance;
	private final HashMap<Integer, Integer> variations;

	public VariationGroup(int chance, String vars)
	{
		this.chance = chance;
		String[] variationInfo = vars.split(";");
		variations = new HashMap<>(variationInfo.length);
		for(String vv : variationInfo)
		{
			String[] vi = vv.split(",");
			variations.put(Integer.parseInt(vi[0]), (int)(Float.parseFloat(vi[1]) * 100000));
		}
	}

	public int getChance()
	{
		return chance;
	}

	public int getRandomOption()
	{
		int chance = Rnd.get(10000000);
		if(Config.DEBUG)
			_log.info("VariationData: getRandomOption: " + chance);

		for(Map.Entry<Integer, Integer> entry : variations.entrySet())
		{
			if(Config.DEBUG)
				_log.info("VariationData: getRandomOption: option: " + entry.getKey() + " chance: " + entry.getValue());
			if(chance < entry.getValue())
				return entry.getKey();

			chance -= entry.getValue();
		}

		_log.warn("VariationData: no random option generated: " + variations.size());
		return 0;
	}
}
