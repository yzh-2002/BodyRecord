"""Build the offline Chinese food guide from official USDA SR Legacy CSVs."""
import csv
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
RAW=next((ROOT/'.tools/foods/usda').glob('*'))
# FDC ID | Chinese label | category | weighed state | introduction | interpretation
ROWS="""
169757|白米饭|碳水|熟、无盐|常见主食，以碳水化合物为主要营养来源。|这是煮熟米饭的可食重量，不是生米重量。
169704|糙米饭|碳水|熟|保留较多谷物结构的主食，也含少量蛋白质和脂肪。|含水量和米种会改变每 100 克的数值。
173904|燕麦片|碳水|干、无糖原味|谷物类碳水来源，也提供蛋白质和脂肪。|按干燕麦称重；加水煮熟后的每 100 克数值不同。
168484|红薯|碳水|水煮、去皮|薯类碳水来源，可作为主食的一部分。|按水煮后去皮的可食部分称重。
170440|土豆|碳水|水煮、去皮、无盐|薯类碳水来源，本条目不包含额外烹调用油。|炸薯条或加油土豆不适用本条数据。
172688|全麦面包|碳水|成品、通用配方|谷物类碳水来源，也含蛋白质和少量脂肪。|不同品牌的糖、油和水分含量不同，优先看实际包装。
173944|香蕉|碳水|生、去皮可食部分|水果类碳水来源，数据按果肉重量。|称重时不包含香蕉皮。
169999|甜玉米粒|碳水|水煮、沥干、无盐|淀粉类食物来源，也含少量蛋白质和脂肪。|按玉米粒可食重量，不包含玉米芯。
171477|鸡胸肉|蛋白质|熟、去皮、烤制|常见禽肉蛋白质来源，本条目为去皮肉。|按熟重；不能与生鸡胸肉每 100 克数据直接混用。
173424|水煮鸡蛋|蛋白质|熟、去壳全蛋|同时提供蛋白质和脂肪，不是纯蛋白质食物。|重量不含蛋壳；一枚鸡蛋不一定等于 100 克。
171956|鳕鱼|蛋白质|熟、干热烹调|鱼类蛋白质来源，不包含裹粉或额外酱汁。|不同鱼种与烹调方式的数据不同。
171998|三文鱼|蛋白质|熟、野生大西洋鲑|同时含蛋白质和脂肪的鱼类食物。|养殖、野生和具体鱼种的脂肪含量可能不同。
175180|熟虾仁|蛋白质|熟、可食部分|水产类蛋白质来源。|不包含虾壳和额外烹调用油。
168634|瘦牛肉|蛋白质|熟、上腰部、可分离瘦肉|畜肉蛋白质来源，本条目已修去可分离脂肪。|部位和肥瘦比例会明显影响脂肪含量。
172475|北豆腐（硬豆腐参考）|蛋白质|原味、硬豆腐|植物蛋白来源，也含一定脂肪。|采用 USDA 硫酸钙硬豆腐条目，不同本地豆腐含水量不同。
173432|脱脂牛奶|蛋白质|液态、按克计|奶类蛋白质来源，同时含乳糖。|本条按 100 克，不直接将 100 毫升视作精确相同重量。
170894|原味脱脂希腊酸奶|蛋白质|成品、原味无脂|乳制品蛋白质来源，也含一定碳水。|调味、加糖和不同脂肪版本应以标签为准。
174271|熟黄豆|蛋白质|水煮、无盐|植物蛋白来源，同时含碳水和脂肪。|这是煮熟黄豆，不是干黄豆或豆浆。
171413|橄榄油|脂肪|食用油|油脂类食物，几乎全部由脂肪构成。|按克称重；一勺的容量和实际重量并不固定。
171411|大豆油|脂肪|食用油|常见烹调用油，以脂肪为主要营养成分。|烹调时实际吃进的油量取决于食物残留和分餐。
170567|杏仁|脂肪|原味、可食坚果仁|坚果类脂肪来源，同时含蛋白质与碳水。|不是杏核仁；加糖、油炸或调味版本以标签为准。
170187|核桃仁|脂肪|原味、可食部分|坚果类脂肪来源，也提供少量蛋白质与碳水。|称重时不包含坚硬外壳。
173806|花生仁|脂肪|干烤、无盐|同时含脂肪、蛋白质与碳水的食物。|本条为烤熟花生仁，不是带壳花生或油炸花生。
170556|南瓜籽仁|脂肪|干、去壳|种子类脂肪来源，同时也含蛋白质。|称重时不包含外壳。
170562|葵花籽仁|脂肪|干、去壳|种子类脂肪来源，也含蛋白质与碳水。|带壳瓜子的总重不等于可食籽仁重量。
171705|牛油果|脂肪|生、去皮去核|脂肪较丰富的水果，同时含水分、碳水和少量蛋白质。|按果肉重量，不包含果核和果皮。
172470|原味花生酱|脂肪|顺滑、无盐|以脂肪为主，也含蛋白质和碳水。|不同品牌添加糖和油的情况不同，优先看实际标签。
"""

def build():
    foods={r['fdc_id']:r for r in csv.DictReader((RAW/'food.csv').open(encoding='utf-8'))}
    nutrients={}
    for r in csv.DictReader((RAW/'food_nutrient.csv').open(encoding='utf-8')):
        if r['nutrient_id'] in ('1003','1004','1005'):
            nutrients.setdefault(r['fdc_id'],{})[r['nutrient_id']]=float(r['amount'])
    result=[]
    for row in ROWS.strip().splitlines():
        identifier,name,category,state,description,note=row.split('|')
        f=foods[identifier];n=nutrients[identifier]
        assert all(key in n for key in ('1003','1004','1005'))
        result.append(dict(id=int(identifier),name=name,category=category,state=state,description=description,note=note,
            sourceName=f['description'],carbs=n['1005'],protein=n['1003'],fat=n['1004']))
    (ROOT/'app/src/main/assets/foods.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    manifest=dict(source='USDA FoodData Central SR Legacy, April 2018',
        download='https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip',
        archiveSha256=hashlib.sha256((ROOT/'.tools/foods/sr-legacy.zip').read_bytes()).hexdigest(),
        nutrientIds={'carbs':1005,'protein':1003,'fat':1004},basis='100 g edible portion; carbohydrate by difference',
        foods=[dict(fdcId=f['id'],sourceName=f['sourceName'],chineseName=f['name']) for f in result])
    (ROOT/'docs/food-source-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Built',len(result),'food entries with verified USDA nutrient rows')
    for f in result:print(f['id'],f['name'],f['carbs'],f['protein'],f['fat'])

if __name__=='__main__':build()
