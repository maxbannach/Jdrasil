target:
	chmod +x make.sh tw*
	./make.sh

test:
	./tw-exact -s 1234 < instances/ClebschGraph.gr
	./tw-exact-parallel -s 1234 < instances/McGeeGraph.gr
	./tw-heuristic -s 1234 < instances/NauruGraph.gr
	./tw-heuristic-parallel -s 1234 < instances/DoubleStarSnark.gr

