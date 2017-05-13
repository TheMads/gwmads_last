package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 16.09.2009 17:51:08
 */
public class i_frintezza_music extends i_effect
{
	public i_frintezza_music(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		/*
		if(!(cha instanceof L2FrintezzaInstance))
			return;

		L2Skill skill = SkillTable.getInstance().getInfo(5008, getSkill().getLevel());
		L2FrintezzaInstance frintezza = (L2FrintezzaInstance) cha;
		switch(getSkill().getLevel())
		{
			case 1:
				skill.applyEffects(frintezza, frintezza.getDemon(), false);
				break;
			case 2:
				skill.applyEffects(cha, frintezza.getDemon(), false);
				for(L2Character character : FrintezzaManager.getInstance().getHallZone().getCharacters())
					if(character != null && (character.getNpcId() == 29048 || character.getNpcId() == 29049))
						skill.applyEffects(frintezza, character, false);
				break;
			case 3:
				skill.applyEffects(frintezza, frintezza.getDemon(), false);
				break;
			case 4:
			case 5:
				for(L2Player player : FrintezzaManager.getInstance().getHallZone().getPlayers())
					skill.applyEffects(frintezza, player, false);
				break;
		}
		*/
	}
}
