package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.category.CategoryManager;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.templates.L2PetTemplate;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 06.11.2010 18:05:03
 */
public class ConditionUseCategory extends Condition
{
	private GArray<Integer> _category;

	public ConditionUseCategory(String category)
	{
		_category = new GArray<>(1);
		String[] cats = category.split(";");
		if(cats.length > 0)
			for(String cat : cats)
			{
				int catId;
				if(cat != null && !cat.isEmpty() && (catId = CategoryManager.getCategoryId(cat)) >= 0)
					_category.add(catId);
			}
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.character instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) env.character;
			for(Integer cat : _category)
				if(CategoryManager.isInCategory(cat, pet))
					return true;

			return false;
		}

		if(env.character instanceof L2Player)
		{
			L2Player player = (L2Player) env.character;
			L2PetTemplate petTemplate = player.getMountEngine().getPetTemplate();
			if(petTemplate != null)
				for(Integer cat : _category)
					if(CategoryManager.isInCategory(cat, petTemplate.npcId))
						return true;
		}

		return false;
	}
}
