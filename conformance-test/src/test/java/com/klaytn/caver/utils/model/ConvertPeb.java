/*
 * Copyright 2021 The caver-java Authors
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klaytn.caver.utils.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.klaytn.caver.utils.Utils;
import org.web3j.protocol.ObjectMapperFactory;

import java.io.IOException;

@JsonDeserialize(using = ConvertPeb.ConvertPebDeserializer.class)
public class ConvertPeb implements IMethodInputParams {
    String num;
    String unit;
    Utils.KlayUnit klayUnit;

    public ConvertPeb(String num, String unit) {
        this.num = num;
        this.unit = unit;
    }

    public ConvertPeb(String num, Utils.KlayUnit klayUnit) {
        this.num = num;
        this.klayUnit = klayUnit;
    }

    @Override
    public Object[] getInputArray() {
        if(unit == null || unit.isEmpty()) {
            return new Object[] {num, klayUnit};
        }
        return new Object[] {num, unit};
    }

    @Override
    public Class[] getInputTypeArray() {
        if(unit == null || unit.isEmpty()) {
            return new Class[] {num.getClass(), klayUnit.getClass()};
        }

        return new Class[] {num.getClass(), unit.getClass()};
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Utils.KlayUnit getKlayUnit() {
        return klayUnit;
    }

    public void setKlayUnit(Utils.KlayUnit klayUnit) {
        this.klayUnit = klayUnit;
    }

    public static class ConvertPebDeserializer extends JsonDeserializer<ConvertPeb> {

        private static ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

        @Override
        public ConvertPeb deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode root = p.getCodec().readTree(p);
            String num = root.get("num").asText();

            if(root.has("unit")) {
                String unit = root.get("unit").asText();
                return new ConvertPeb(num, unit);
            }
//            else {
//                JsonNode klayUnit = root.get("klayUnit");
//                String unit = klayUnit.get("unit").asText();
//                int pebFactor = klayUnit.get("pebFactor").asInt();
//                return new ConvertPeb(num, new Utils.KlayUnit(unit, pebFactor));
//            }
            return null;
        }
    }
}
