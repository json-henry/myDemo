    private void fusionVgsToContentBaseByStrategy(CmsPreProgramsetBase base, String type, MediaSupplementInfo response,
                                                  Map<String, StrategyModelBO> editStrategy) {
        log.info("fusionVgsToContentBaseByStrategy method start");
        if(editStrategy.get(EditStategyConstant.MERGE_STRATEGY) == null){
            return;
        }
        log.info("start fusionVgsToContentBaseByStrategy outSourceId:{}",base.getOutSourceId());
        List<CmsCustomField> list = cmsCustomFieldServiceImpl.listByValid(base.getSetFlag() == 0 ? "0" : "1");
        HashMap<String, String> map = new HashMap<>();
        list.forEach(item->{map.put(item.getFieldName(),item.getIdentifier());});
        List<StrategyModelBO> strategyModels = JSONUtil.toList(editStrategy.get(EditStategyConstant.MERGE_STRATEGY).getValue(),StrategyModelBO.class);
//        log.info("fusionVgsToContentBaseByStrategy method strategyModels is : {}",strategyModels);
        if (CollectionUtil.isEmpty(strategyModels)) {
            return;
        }
        for (StrategyModelBO strategyModelBO : strategyModels) {
            if (!StringUtils.equalsIgnoreCase(Constants.EDIT_STRATEGY_CONTENT_TYPE, strategyModelBO.getKey())) {
                continue;
            }
            List<StrategyModelBO> fusionStrategys = JSONUtil.toList(strategyModelBO.getValue(), StrategyModelBO.class);
            if (CollectionUtil.isEmpty(fusionStrategys)) {
                continue;
            }
            for (StrategyModelBO fusionStrategy : fusionStrategys) {
                if (!StringUtils.equalsIgnoreCase(type, fusionStrategy.getKey())) {
                    continue;
                }
                List<StrategyModelBO> fusionStrategyDetail = JSONUtil.toList(fusionStrategy.getValue(), StrategyModelBO.class);
                if (CollectionUtils.isEmpty(fusionStrategyDetail)) {
                    continue;
                }
                for (StrategyModelBO modelBO : fusionStrategyDetail) {
                    if (!StringUtils.equalsIgnoreCase(Constants.EDIT_STRATEGY_FUSION_TRUE, modelBO.getValue())) {
                        continue;
                    }
                    try {
                        String[] split = modelBO.getKey().split(Constants.FUSION_SPLIT);
                        if (split.length != 2) {
                            continue;
                        }
                        Field vgsField = response.getClass().getDeclaredField(split[1]);
                        vgsField.setAccessible(true);
                        Object vgsObj = vgsField.get(response);
                        if (ObjectUtils.isEmpty(vgsObj)) {
                            continue;
                        }
                        if ("tagList".equals(split[1]) || "genre".equals(split[1])) {
                            vgsObj = ((String) vgsObj).replace("[", "").replace("]", "");
                        }
                        String s = split[0];
                        if (s.contains("custom_")) {
                            // 自定义的字段单独处理
                            this.fillField(base, s, vgsObj, vgsField, map);
                        } else {
                            // 处理非自定义字段
                            Field baseField = base.getClass().getDeclaredField(s);
                            baseField.setAccessible(true);
                            if (vgsField.getType() == baseField.getType()) {
                                baseField.set(base, vgsObj);
                            } else {
                                BeanUtils.setBeanAttributeValue(split[0], base, vgsObj);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Method: fusionVgsToContentBaseByStrategy 字段融合异常", e);
                    }
                }
            }
        }
    }
