package com.dianping.cat.system.config;

import java.util.List;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.unidal.dal.jdbc.DalNotFoundException;
import org.unidal.helper.Files;

import com.dianping.cat.Cat;
import com.dianping.cat.core.config.Config;
import com.dianping.cat.core.config.ConfigEntity;
import com.dianping.cat.home.rule.entity.Condition;
import com.dianping.cat.home.rule.entity.MonitorRules;
import com.dianping.cat.home.rule.entity.Rule;
import com.dianping.cat.home.rule.entity.SubCondition;
import com.dianping.cat.home.rule.transform.DefaultSaxParser;
import com.dianping.cat.report.task.alert.AlertLevel;

public class AppRuleConfigManager extends BaseRuleConfigManager implements Initializable {

	private static final String CONFIG_NAME = "appRule";

	public void addDefultRule(String name, Integer commandId) {
		String ruleId = buildRuleId(name, commandId);
		Rule rule = new Rule(ruleId);

		rule.addConfig(buildDefaultConfig());
		m_config.addRule(rule);
		if (!storeConfig()) {
			Cat.logError("store app command rule error: " + name + " " + " " + commandId, new RuntimeException());
		}
	}

	private com.dianping.cat.home.rule.entity.Config buildDefaultConfig() {
		com.dianping.cat.home.rule.entity.Config config = new com.dianping.cat.home.rule.entity.Config();
		config.setStarttime("00:00");
		config.setEndtime("24:00");

		Condition condition = new Condition();
		condition.setAlertType(AlertLevel.WARNING);
		condition.setMinute(3);
		SubCondition minSuccessSubCondition = new SubCondition();

		minSuccessSubCondition.setType("MinVal").setText("95");
		condition.addSubCondition(minSuccessSubCondition);
		config.addCondition(condition);

		return config;
	}

	private String buildRuleId(String name, Integer commandId) {
		return commandId + ";-1;-1;-1;-1;-1;-1;-1:success:" + name;
	}

	public void deleteDefaultRule(String name, List<Integer> commandIds) {
		for (int commandId : commandIds) {
			String ruleId = buildRuleId(name, commandId);

			m_config.removeRule(ruleId);
		}

		if (!storeConfig()) {
			Cat.logError("delete app command rule error: " + name, new RuntimeException());
		}
	}

	@Override
	protected String getConfigName() {
		return CONFIG_NAME;
	}

	@Override
	public void initialize() throws InitializationException {
		try {
			Config config = m_configDao.findByName(CONFIG_NAME, ConfigEntity.READSET_FULL);
			String content = config.getContent();

			m_configId = config.getId();
			m_config = DefaultSaxParser.parse(content);
		} catch (DalNotFoundException e) {
			try {
				String content = Files.forIO().readFrom(
				      this.getClass().getResourceAsStream("/config/default-app-rule-config.xml"), "utf-8");
				Config config = m_configDao.createLocal();

				config.setName(CONFIG_NAME);
				config.setContent(content);
				m_configDao.insert(config);

				m_configId = config.getId();
				m_config = DefaultSaxParser.parse(content);
			} catch (Exception ex) {
				Cat.logError(ex);
			}
		} catch (Exception e) {
			Cat.logError(e);
		}
		if (m_config == null) {
			m_config = new MonitorRules();
		}
	}

}
