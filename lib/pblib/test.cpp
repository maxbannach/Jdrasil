#include "pb2cnf.h"
#include "VectorClauseDatabase.h"
#include <vector>

using namespace PBLib;  
int main(int argc, char *argv[])
{

  PBConfig config = std::make_shared< PBConfigClass >();
  VectorClauseDatabase formula(config);
  PB2CNF pb2cnf(config);
  AuxVarManager auxvars(11);
   
  std::vector< WeightedLit > literals =
  {WeightedLit(1,-7), WeightedLit(-2,5), WeightedLit(-3,9), WeightedLit(-10,-3), WeightedLit(10,7)};
    
   
  IncPBConstraint constraint(literals, BOTH, 100, -5);
   
  pb2cnf.encodeIncInital(constraint, formula, auxvars);

  int m = formula.getClauses().size();

  constraint.encodeNewGeq(3, formula, auxvars);

  std::vector<std::vector<int32_t> > clauses = formula.getClauses();

  int mp = clauses.size();
  for (int i = m; i < mp; i++){
    std::vector<int32_t> clause = clauses[i];
    for (auto var: clause){
      std::cout << var << ' ';
    }
    std::cout << "\n";
  }

  return 0;
}
