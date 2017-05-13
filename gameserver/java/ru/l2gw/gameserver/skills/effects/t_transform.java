package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 26.07.2010 14:49:45
 */
public class t_transform extends t_effect
{
	private final int _maleNpcId, _femaleNpcId;
	private final int _type;

	public t_transform(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
		_maleNpcId = template._attrs.getInteger("npcId", 0);
		_femaleNpcId = template._attrs.getInteger("femaleNpcId", _maleNpcId);
		_type = template._attrs.getInteger("type", 0);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(!getEffected().isPlayer())
			return;

		L2Player player = (L2Player) getEffected();
		if(player.getTransformation() > 0)
			return;

		for(L2Effect effect : player.getAllEffectsArray())
			if(effect.getSkill().isToggle())
				effect.exit();

		if(_type == 1)
			player.getMountEngine().setMount(getSkill().getNpcId());

		int id = (int) calc();
		player.setTransformationTemplate(player.getSex() == 0 ? _maleNpcId : _femaleNpcId);
		player.setTransformation(id);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(!getEffected().isPlayer())
			return;

		L2Player player = (L2Player) getEffected();
		if(player.getTransformation() == 0)
			return;

		player.setTransformation(0);
		if(_type == 1)
			player.getMountEngine().dismount();
		player.setTransformationTemplate(0);
	}
}
