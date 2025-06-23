//package com.example.adoption_and_breeding_module.service.impl;
//
//import ai.djl.Application;
//import ai.djl.ModelException;
//import ai.djl.inference.Predictor;
//import ai.djl.modality.Classifications;
//import ai.djl.modality.Classifications.Classification;
//import ai.djl.repository.zoo.Criteria;
//import ai.djl.repository.zoo.ModelZoo;
//import ai.djl.repository.zoo.ZooModel;
//import ai.djl.training.util.ProgressBar;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//
//@Service
//public class ToxicityChecker {
//
//    private ZooModel<String, Classifications> model;
//    private Predictor<String, Classifications> predictor;
//
//    @PostConstruct
//    public void init() throws ModelException, IOException {
//        Criteria<String, Classifications> criteria = Criteria.builder()
//                .optApplication(Application.NLP.TEXT_CLASSIFICATION)
//                .setTypes(String.class, Classifications.class)
//                .optArtifactId("toxicity")
//                .optProgress(new ProgressBar())
//                .build();
//
//        model = ModelZoo.loadModel(criteria);
//        predictor = model.newPredictor();  // predictor depends on model
//    }
//
//    @PreDestroy
//    public void destroy() {
//        try {
//            if (predictor != null) predictor.close();
//            if (model != null) model.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Returns true if the text is classified as toxic with >50% probability.
//     */
//    public boolean isToxic(String text) throws Exception {
//        Classifications result = predictor.predict(text);
//        for (Classification cls : result.items()) {
//            if ("toxic".equalsIgnoreCase(cls.getClassName()) && cls.getProbability() > 0.5) {
//                return true;
//            }
//        }
//        return false;
//    }
//}