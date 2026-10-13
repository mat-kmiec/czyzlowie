package pl.matkmiec.backend.fish.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LegalStatus {
    NATIVE("Gatunek rodzimy (regulowany RAPR/PZW)", "Gatunek objęty standardowymi przepisami Regulaminu Amatorskiego Połowu Ryb"),
    STRICTLY_PROTECTED("Ochrona ścisła całoroczna", "Gatunek objęty ścisłą ochroną gatunkową – bezwzględny zakaz zabierania i nakaz natychmiastowego ostrożnego wypuszczenia"),
    INVASIVE_NO_RELEASE("Obcy / inwazyjny – bezwzględny zakaz wypuszczania", "Zgodnie z polskim prawem ryb tego gatunku po złowieniu nie wolno wpuszczać z powrotem do łowiska ani do innych wód"),
    COMMERCIAL_REGULATED("Gatunek poławiany gospodarczo i amatorsko", "Gatunek poławiany w wodach śródlądowych i morskich podlegający regulacjom obwodów rybackich");

    private final String displayName;
    private final String description;
}
