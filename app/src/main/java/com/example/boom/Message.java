package com.example.boom;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

public class Message {
    private static final String[] greetings = {
            "您好，",
            "你好医生，",
            "您好医生，",
            "紧急求助：",
            "哈喽！",
            "嗨，你好！",
            "你好，"
    };

    private static final String[] unknownPatients = {
            "我本人",
            "我小孩",
            "我孩子",
            "我邻居的小孩",
            "我邻居的孩子",
            "我亲戚",
            "我的远房亲戚",
            "我同事",
            "我同事的小孩",
            "我同事的孩子",
            "我邻居",
            "我邻居的老人"
    };

    private static final String[] femalePatients = {
            "我妻子",
            "我老婆",
            "我媳妇",
            "我女儿",
            "我孙女",
            "我妈妈",
            "我母亲",
            "我婆婆",
            "我奶奶",
            "我大娘",
            "我大姨",
            "我小姨",
            "我婶婶",
            "我婶子",
            "我堂姐",
            "我堂妹",
            "我侄女",
            "我外婆",
            "我姥姥",
            "我岳母",
            "我舅妈",
            "我表姐",
            "我表妹",
            "我表嫂",
            "我阿姨",
            "我姑姑"
    };

    private static final String[] malePatients = {
            "我儿子",
            "我孙子",
            "我爸爸",
            "我爷爷",
            "我外公",
            "我姥爷",
            "我老公",
            "我丈夫",
            "我父亲",
            "我大舅",
            "我堂兄",
            "我堂弟",
            "我小叔",
            "我叔叔",
            "我大伯",
            "我表叔",
            "我表侄",
            "我表外甥",
            "我侄子",
            "我表弟",
            "我表兄",
            "我表哥",
            "我岳父",
            "我公公",
            "我舅舅",
            "我姑父",
            "我姨父"
    };

    private static final String[] unknownSymptoms = {
            "最近经常感到头痛，",
            "最近血压有点高，",
            "最近消化不良，",
            "最近经常失眠，",
            "最近感觉胸口闷，",
            "最近体重下降得很快，",
            "最近经常感到疲劳，",
            "最近经常感到头晕，",
            "最近皮肤出现红疹，",
            "最近关节疼痛，",
            "最近经常感到焦虑，",
            "最近经常感到口渴，",
            "最近经常感到心跳加速，",
            "最近经常感到记忆力下降，",
            "最近经常感到呼吸困难，",
            "最近经常感到肌肉酸痛，",
            "最近经常感到情绪低落，",
            "最近经常感到眼睛干涩，",
            "最近经常感到眼睛视力模糊，",
            "最近经常感到皮肤瘙痒，",
            "最近经常感到耳鸣，",
            "最近食欲不振，",
            "最近便秘，",
            "最近长了个痔疮，",
            "最近尿频，",
            "最近夫妻生活不和谐，",
            "最近一直便血，",
            "不孕不育",
            "最近尿急，",
            "最近尿痛，",
            "最近腹部胀痛，",
            "最近身体疼痛，",
            "最近恶心，",
            "最近身体发热，",
            "最近身体乏力，",
            "最近身体虚弱，",
            "最近身体一直不舒服"
    };

    private static final String[] femaleSymptoms = {
            "最近月经失调，",
            "最近痛经，",
            "胎儿畸形，想做引产手术，",
            "宫颈糜烂"
    };

    private static final String[] maleSymptoms = {
            "一直包皮过长，",
            "最近阳痿早泄，",
            "最近前列腺发炎"
    };

    private static final String[] requests = {
            "急需医疗建议，",
            "急需救助，",
            "需要治疗，",
            "需要帮助，",
            "需要您的帮助，",
            "需要您的建议，",
            "想咨询一下，",
            "非常担心，想咨询一下，",
            "需要您的专业意见，",
            "不知道是否严重，想问一下，"
    };

    private static final String[] contactInfos = {
            "请尽快联系：",
            "请尽快与我联系：",
            "请速回电：",
            "电话：",
            "手机：",
            "联系方式：",
            "可以打我电话：",
            "可以电话详谈：",
            "我的号码：",
    };

    public static String generateMessage(String phoneNumber) {
        Random random = new Random();
        int greetingIndex = random.nextInt(greetings.length);
        final String[] patients = Stream.of(unknownPatients, femalePatients, malePatients)
                .flatMap(Arrays::stream)
                .toArray(String[]::new);
        int patientIndex = random.nextInt(patients.length);
        final String patient = patients[patientIndex];
        String symptom;
        if (Arrays.asList(unknownPatients).contains(patient)) {
            final String[] symptoms = Stream.of(unknownSymptoms, femaleSymptoms, maleSymptoms)
                    .flatMap(Arrays::stream)
                    .toArray(String[]::new);
            int symptomIndex = random.nextInt(symptoms.length);
            symptom = symptoms[symptomIndex];
        } else if (Arrays.asList(femalePatients).contains(patient)) {
            final String[] symptoms = Stream.of(unknownSymptoms, femaleSymptoms)
                    .flatMap(Arrays::stream)
                    .toArray(String[]::new);
            int symptomIndex = random.nextInt(symptoms.length);
            symptom = symptoms[symptomIndex];
        } else {
            final String[] symptoms = Stream.of(unknownSymptoms, maleSymptoms)
                    .flatMap(Arrays::stream)
                    .toArray(String[]::new);
            int symptomIndex = random.nextInt(symptoms.length);
            symptom = symptoms[symptomIndex];
        }

        int requestIndex = random.nextInt(requests.length);
        int contactInfoIndex = random.nextInt(contactInfos.length);

        return greetings[greetingIndex] + patients[patientIndex] + symptom
                + requests[requestIndex] + contactInfos[contactInfoIndex]
                + phoneNumber;
    }
}
