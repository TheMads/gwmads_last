package ru.l2gw.gameserver.model.variation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: rage
 * @date: 18.10.11 21:16
 */
public class Variation
{
	private static final Pattern varPattern = Pattern.compile("\\{(.+?)\\}(.+)");
	private static final Log _log = LogFactory.getLog("variation");

	private final boolean isMagic;
	private final int mineralId;
	private GArray<VariationGroup> variation1;
	private GArray<VariationGroup> variation2;

	public Variation(int mineralId, boolean isMagic)
	{
		this.mineralId = mineralId;
		this.isMagic = isMagic;
	}

	public void setVariation1(GArray<VariationGroup> vars)
	{
		variation1 = vars;
	}

	public void setVariation2(GArray<VariationGroup> vars)
	{
		variation2 = vars;
	}

	public int getMineralId()
	{
		return mineralId;
	}

	public boolean isMagicWeapon()
	{
		return isMagic;
	}

	public int getRandomVariation()
	{
		int chance = Rnd.get(10000000);
		VariationGroup vg1 = null;
		if(Config.DEBUG)
			_log.info("random variation1 chance: " + chance);
		for(VariationGroup vg : variation1)
		{
			if(Config.DEBUG)
				_log.info("random variation1 group: " + vg.getChance());
			if(chance < vg.getChance())
			{
				vg1 = vg;
				break;
			}
			chance -= vg.getChance();
		}

		if(vg1 == null)
		{
			_log.warn("VariationData: no random variation1 generated WTF?? size: " + variation1.size());
			return 0;
		}

		chance = Rnd.get(10000000);
		VariationGroup vg2 = null;
		if(Config.DEBUG)
			_log.info("random variation2 chance: " + chance);
		for(VariationGroup vg : variation2)
		{
			if(Config.DEBUG)
				_log.info("random variation2 group: " + vg.getChance());
			if(chance < vg.getChance())
			{
				vg2 = vg;
				break;
			}
			chance -= vg.getChance();
		}

		if(vg2 == null)
		{
			_log.warn("VariationData: no random variation2 generated WTF?? size: " + variation2.size());
			return 0;
		}


		int stat1 = vg1.getRandomOption();
		int stat2 = vg2.getRandomOption();

		if(Config.DEBUG)
			_log.info("VariationData: stat1: " + stat1 + " stat2: " + stat2);
		return ((stat2 << 16) + stat1);
	}

	public static GArray<VariationGroup> parseGroup(String variation)
	{
		StringTokenizer st = new StringTokenizer(variation);
		GArray<VariationGroup> variations = new GArray<>(st.countTokens());
		Matcher matcher;
		while(st.hasMoreTokens())
		{
			String var = st.nextToken();
			matcher = varPattern.matcher(var);
			if(matcher.find())
			{
				String vars = matcher.group(1);
				int chance = (int)(Float.parseFloat(matcher.group(2)) * 100000);
				variations.add(new VariationGroup(chance, vars));
			}
		}

		return variations;
	}
}
