package com.example.smilestock.service;

import com.example.smilestock.entity.StockEntity;
import com.example.smilestock.repository.StockRepository;
import io.github.flashvayne.chatgpt.service.ChatgptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ChatService {
    private final ChatgptService chatgptService;
    private final StockRepository stockRepository;

    @Autowired
    public ChatService(ChatgptService chatgptService, StockRepository stockRepository) {
        this.chatgptService = chatgptService;
        this.stockRepository = stockRepository;
    }
    @Value("${dart.api-key}")
    private String dartApiKey;


    public String getChatResponse(String prompt) {
        // ChatGPT 에게 질문을 던집니다.
        return chatgptService.sendMessage(prompt);
    }

    // 종목 코드 및 기업 코드 가져오기(DB 저장)
    public ResponseEntity<?> getCorpInfo() {
        try {
            // url로 요청 후 binary 데이터 가져오기
            String url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + dartApiKey;
            RestTemplate restTemplate = new RestTemplate();
            byte[] zipBytes = restTemplate.getForObject(url, byte[].class);

            // 압축 해제
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry zipEntry = zis.getNextEntry();

                if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".xml")) {
                    // XML 파일 파싱
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(new InputSource(zis));
                    doc.getDocumentElement().normalize();

                    // list태그를 찾아 해당 노드들 순회
                    NodeList nList = doc.getElementsByTagName("list");
                    for (int temp = 0; temp < nList.getLength(); temp++) {
                        Node node = nList.item(temp);

                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element element = (Element) node;

                            String corpCode = element.getElementsByTagName("corp_code").item(0).getTextContent();
                            String stockCode = element.getElementsByTagName("stock_code").item(0).getTextContent().trim();

                            if (!stockCode.isEmpty()) {
                                // 주식 코드가 이미 데이터베이스에 존재하는지 확인
                                Optional<StockEntity> existingStock = stockRepository.findByStockCode(stockCode);
                                if (existingStock.isEmpty()) {
                                    StockEntity stockEntity = new StockEntity();
                                    stockEntity.setCorpCode(corpCode);
                                    stockEntity.setStockCode(stockCode);
                                    stockRepository.save(stockEntity);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while processing corp info: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("오류가 발생했습니다.");
        }
        return ResponseEntity.status(200).body("정상적으로 저장되었습니다.");
    }
}
