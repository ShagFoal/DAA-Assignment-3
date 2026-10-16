import copy
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
FOLDER = ROOT / 'tests' / 'student'
sys.path.insert(0, str(ROOT / 'tools'))
from run import build, execute
from public_validate import check

cases = json.loads((FOLDER / 'expected.json').read_text())
inputs = [json.loads(line) for line in (FOLDER / 'own_cases.jsonl').read_text().splitlines()]
assert inputs == [case['input'] for case in cases]
answers = execute(build('Java'), inputs + list(reversed(inputs)))
for case, answer in zip(cases + list(reversed(cases)), answers):
    assert check(case['input'], answer, case['output']), case['name']
(FOLDER / 'actual.jsonl').write_text(''.join(json.dumps(x) + '\n' for x in answers[:len(cases)]))
print(f'PASS {len(cases)} own cases and {len(answers)} mixed/repeated requests')

for case in cases:
    name, x, expected = case['name'], case['input'], case['output']
    good, bad = copy.deepcopy(expected), copy.deepcopy(expected)
    if name.endswith('ties'):
        good['edge_ids'], bad['edge_ids'] = [1, 2], [0, 0]
    elif name == 'SCC condensation':
        good['component'], good['condensation'] = [7, 7, 8, 9, 10], [[7, 8], [9, 10]]
        bad['condensation'] = []
    elif name == 'Topo diamond':
        good['order'], bad['order'] = [0, 2, 1, 3], [3, 2, 1, 0]
    elif name == 'DAG routes':
        good['parent_edge'], bad['parent_edge'] = [-1, 1, 2, 4, -1], [-1, 1, 0, 4, -1]
    else:
        continue
    assert check(x, good, expected), name
    assert not check(x, bad, expected), name
print('PASS alternative valid answers accepted; wrong witnesses rejected')
