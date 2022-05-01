import sys
import pandas as pd
from scipy.stats import ttest_rel
import pingouin as pt

results_path = 'results/'

ratio_string = sys.argv[1]
ratio = ratio_string.split(',')

sim_string = sys.argv[3]
sim_tokens = sim_string.split(',')
simulations = [int(x) for x in sim_tokens]
no_of_simulations = len(simulations)

emergent_norms = True

if emergent_norms:
    middle_part_of_file = '_emergent_norms'
else:
    middle_part_of_file = ''

trial_string = sys.argv[4]
trials = trial_string.split(',')
trials = [int(x) for x in trials]

def get_payoff_scheme_token(payoff_scheme):
    if payoff_scheme == '2':
        return '_updated_base'
    elif payoff_scheme == '3':
        return '_multi_updated_base'
    else:
        return ''


def get_file_name(sim, ratio, payoff_scheme, trial, emergent_norms = True):
    if (trial == 0):
        trial_token = ''
    else:
        trial_token = '_trial' + str(trial)
    if emergent_norms:
        emergency_norm_token = '_emergent_norms'
        ext = '.txt'
    else:
        emergency_norm_token = ''
        ext = '.csv'
    return results_path + 'Results_Sim' + str(sim) + '_' + '_'.join(ratio) \
            + get_payoff_scheme_token(payoff_scheme) + trial_token + emergency_norm_token + ext


def extract_stats(file_name):
    payoff = None
    coh = None
    payoff_done = False
    coh_done = False
    with open(file_name, 'r') as f:
        for line in f:
            accept_rate_key = 'acceptanceRate'
            cohesion_key = 'cohesion'
            if line.startswith('Avg payoff'):
                payoff = float(line[12:])
                payoff_done = True
                if (coh_done):
                    break
            elif ((not coh_done) and ((line.startswith('[total') and (cohesion_key in line)) or (accept_rate_key in line))):
                    ind = line.find(accept_rate_key)
                    if (ind == -1):
                        ind = line.find(cohesion_key)
                        ind = ind + len(cohesion_key) + 2
                    else:
                        ind = ind + len(accept_rate_key) + 2
                    end = ind
                    while ((line[end] >= '0' and line[end] <= '9') or (line[end] == '.')):
                        end = end + 1
                    coh = float(line[ind:end])
                    coh_done = True
                    if (payoff_done):
                        break
                        
    return payoff, coh

soc_ex_data = []
soc_coh_data = []

if len(sys.argv) <= 2:
    payoff_scheme = '1'
else:
    payoff_scheme = sys.argv[2]

for trial in trials:
    soc_ex_row = []
    soc_coh_row = []
    for sim in simulations:
        fil = get_file_name(sim, ratio, payoff_scheme, trial)
        soc_ex, soc_coh = extract_stats(fil)
        soc_ex_row.append(soc_ex)
        soc_coh_row.append(soc_coh)
    soc_ex_data.append(soc_ex_row)
    soc_coh_data.append(soc_coh_row)

soc_ex_df = pd.DataFrame(soc_ex_data, columns = sim_tokens)
soc_coh_df = pd.DataFrame(soc_coh_data, columns = sim_tokens)

print(soc_ex_df)

print()
print(soc_coh_df)

def get_stat_analysis_util(trial_sim_df):
    sims = trial_sim_df.columns
    baselines = sims[:-1]
    proposed = sims[-1]
    data = []
    for sim in baselines:
        row = []
        col_base = trial_sim_df[sim]
        col_prop = trial_sim_df[proposed]
        row.append(trial_sim_df[sim].min())
        row.append(trial_sim_df[sim].max())
        row.append(trial_sim_df[sim].mean())
        row.append(trial_sim_df[sim].std())
        stats_result = pt.ttest(col_base, col_prop, paired = True)
        row.append(stats_result['p-val']['T-test'])
        row.append(stats_result['cohen-d']['T-test'])
        data.append(row)
    data.append([trial_sim_df[proposed].min(), trial_sim_df[proposed].max(), trial_sim_df[proposed].mean(), trial_sim_df[proposed].std(), None, None])
    num_sims = [int(x) for x in sims]
    return pd.DataFrame(data, index = num_sims, columns = ['Min', 'Max', 'Mean', 'STD', 'p-value', 'cohen\'s d'])

def get_stat_analysis(soc_ex_df, soc_coh_df):
    return get_stat_analysis_util(soc_ex_df), get_stat_analysis_util(soc_coh_df)

ex_res, coh_res = get_stat_analysis(soc_ex_df, soc_coh_df)
print('Social experience result')
print(ex_res.round(2))

print()
print('Social cohesion result')
print(coh_res.round(2))
